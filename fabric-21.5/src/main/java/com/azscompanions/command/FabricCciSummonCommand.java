package com.azscompanions.command;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.cci.CciCompanionParams;
import com.azscompanions.cci.CciMessages;
import com.azscompanions.config.FabricServerConfig;
import com.azscompanions.entity.CompanionAttitude;
import com.azscompanions.entity.CompanionCciSummonSupport;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.entity.FabricCompanionRecruitment;
import com.azscompanions.entity.inventory.FabricCompanionInventory;
import com.azscompanions.item.FabricCompanionCharmItem;
import com.azscompanions.util.CompanionArmorRules;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * CCI / streamer temporary summon.
 * <p>
 * Syntax (permission 2):
 * {@code /az summon [type] [player] [durationSeconds] [health] [armor] [weapon] [tool] [shield] [mode] [name]}
 * <p>
 * Omitted type (or {@code player}) is a player-form companion with a random Steve or Alex skin.
 * Explicit {@code kon}/{@code bits}/{@code wiggly} keep those appearances. Use {@code -} to skip
 * an optional equipment/health token. Duration {@code 0} disables expiry (testing). Default
 * duration is {@code cciSummonDurationSeconds} (90). Charm companions are never spawned or expired
 * by this command.
 */
public final class FabricCciSummonCommand {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private FabricCciSummonCommand() {
    }

    /** Allows {@code namespace:path} — Brigadier {@code word()} rejects {@code :}. */
    private static ArgumentType<String> itemIdArg() {
        return new ArgumentType<>() {
            @Override
            public String parse(StringReader reader) throws CommandSyntaxException {
                if (!reader.canRead()) {
                    return "";
                }
                char c = reader.peek();
                if (c == '"' || c == '\'') {
                    return reader.readString();
                }
                int start = reader.getCursor();
                while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
                    reader.skip();
                }
                return reader.getString().substring(start, reader.getCursor());
            }
        };
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildBranch() {
        var nameArg = Commands.argument("name", StringArgumentType.greedyString())
                .executes(ctx -> equipped(ctx, modeOrNull(ctx), StringArgumentType.getString(ctx, "name")));
        var modeArg = Commands.argument("mode", StringArgumentType.word())
                .executes(ctx -> equipped(ctx, modeOrNull(ctx), null))
                .then(nameArg);
        var shieldArg = Commands.argument("shield", itemIdArg())
                .executes(ctx -> equipped(ctx, null, null))
                .then(modeArg);
        var toolArg = Commands.argument("tool", itemIdArg())
                .executes(ctx -> run(ctx, player(ctx), dur(ctx), hp(ctx), armor(ctx), weapon(ctx),
                        StringArgumentType.getString(ctx, "tool"), null, null, null))
                .then(shieldArg);
        var weaponArg = Commands.argument("weapon", itemIdArg())
                .executes(ctx -> run(ctx, player(ctx), dur(ctx), hp(ctx), armor(ctx),
                        StringArgumentType.getString(ctx, "weapon"), null, null, null, null))
                .then(toolArg);
        var armorArg = Commands.argument("armor", itemIdArg())
                .executes(ctx -> run(ctx, player(ctx), dur(ctx), hp(ctx),
                        StringArgumentType.getString(ctx, "armor"), null, null, null, null, null))
                .then(weaponArg);
        var healthArg = Commands.argument("health", IntegerArgumentType.integer(1, (int) CompanionCciSummonSupport.MAX_HEALTH_VALUE))
                .executes(ctx -> run(ctx, player(ctx), dur(ctx), hp(ctx), null, null, null, null, null, null))
                .then(armorArg);
        var durationArg = Commands.argument("durationSeconds", IntegerArgumentType.integer(0, CompanionCciSummonSupport.MAX_DURATION_SECONDS))
                .executes(ctx -> run(ctx, player(ctx), dur(ctx), -1, null, null, null, null, null, null))
                .then(healthArg);
        var playerArg = Commands.argument("player", EntityArgument.player())
                .executes(ctx -> run(ctx, player(ctx), defaultDuration(), -1, null, null, null, null, null, null))
                .then(durationArg);
        return Commands.literal("summon")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> run(ctx, null, defaultDuration(), -1, null, null, null, null, null, null))
                .then(killBranch())
                .then(Commands.argument("type", StringArgumentType.word())
                        .executes(ctx -> run(ctx, null, defaultDuration(), -1, null, null, null, null, null, null))
                        .then(playerArg));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> killBranch() {
        return Commands.literal("kill")
                .then(Commands.literal("all")
                        .executes(ctx -> killAll(ctx, null))
                        .then(Commands.argument("owner", EntityArgument.player())
                                .executes(ctx -> killAll(ctx, EntityArgument.getPlayer(ctx, "owner")))))
                .then(Commands.literal("nearest")
                        .executes(FabricCciSummonCommand::killNearest))
                .then(Commands.argument("summonName", StringArgumentType.greedyString())
                        .executes(ctx -> killNamed(ctx, StringArgumentType.getString(ctx, "summonName"))));
    }

    private static int equipped(
            CommandContext<CommandSourceStack> ctx,
            @Nullable String mode,
            @Nullable String name) throws CommandSyntaxException {
        return run(ctx, player(ctx), dur(ctx), hp(ctx), armor(ctx), weapon(ctx),
                StringArgumentType.getString(ctx, "tool"),
                StringArgumentType.getString(ctx, "shield"),
                mode,
                name);
    }

    private static int dur(CommandContext<CommandSourceStack> ctx) {
        return IntegerArgumentType.getInteger(ctx, "durationSeconds");
    }

    private static int hp(CommandContext<CommandSourceStack> ctx) {
        return IntegerArgumentType.getInteger(ctx, "health");
    }

    private static String armor(CommandContext<CommandSourceStack> ctx) {
        return StringArgumentType.getString(ctx, "armor");
    }

    private static String weapon(CommandContext<CommandSourceStack> ctx) {
        return StringArgumentType.getString(ctx, "weapon");
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return EntityArgument.getPlayer(ctx, "player");
    }

    private static int defaultDuration() {
        return FabricServerConfig.CCI_SUMMON_DURATION_SECONDS;
    }

    private static String typeOrDefault(CommandContext<CommandSourceStack> ctx) {
        try {
            return StringArgumentType.getString(ctx, "type");
        } catch (IllegalArgumentException ex) {
            return CompanionCciSummonSupport.DEFAULT_TYPE;
        }
    }

    private static String modeOrNull(CommandContext<CommandSourceStack> ctx) {
        try {
            return StringArgumentType.getString(ctx, "mode");
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static int run(
            CommandContext<CommandSourceStack> ctx,
            @Nullable ServerPlayer target,
            int durationSeconds,
            int health,
            @Nullable String armor,
            @Nullable String weapon,
            @Nullable String tool,
            @Nullable String shield,
            @Nullable String mode,
            @Nullable String name) throws CommandSyntaxException {
        ServerPlayer owner = target != null ? target : ctx.getSource().getPlayerOrException();
        String type = typeOrDefault(ctx);
        String[] modeName = CompanionCciSummonSupport.splitModeAndName(mode, name);
        FabricCompanionEntity spawned = spawn(
                owner,
                type,
                durationSeconds,
                health > 0 ? (float) health : null,
                armor,
                weapon,
                tool,
                shield,
                modeName[1],
                null,
                modeName[0]);
        if (spawned == null) {
            ctx.getSource().sendFailure(Component.translatable("message.azscompanions.cci.cci_summon_failed"));
            return 0;
        }
        int shownDuration = CompanionCciSummonSupport.clampDurationSeconds(durationSeconds);
        String window = shownDuration <= 0 ? "no expiry" : shownDuration + "s";
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "message.azscompanions.cci.cci_summon_ok",
                spawned.getChatDisplayName(),
                owner.getGameProfile().getName(),
                window), true);
        return 1;
    }

    /**
     * CCI IMC / {@code /azscci companion_cci_summon} entry. Always owned by the streamer player.
     */
    @Nullable
    public static FabricCompanionEntity spawnFromCci(ServerPlayer streamer, CciCompanionParams params) {
        if (streamer == null) {
            return null;
        }
        int duration = params.summonDurationSecondsOr(defaultDuration());
        return spawn(
                streamer,
                params.companionTypeOr(CompanionCciSummonSupport.DEFAULT_TYPE),
                duration,
                params.healthOrNull(),
                params.armorSpec(),
                params.weaponItem(),
                params.toolItem(),
                params.shieldItem(),
                params.summonDisplayName(),
                params.skinUsername(),
                params.behaviorModeOr(CompanionCciSummonSupport.DEFAULT_MODE));
    }

    @Nullable
    public static FabricCompanionEntity spawn(
            ServerPlayer owner,
            String type,
            int durationSeconds,
            @Nullable Float health,
            @Nullable String armor,
            @Nullable String weapon,
            @Nullable String tool,
            @Nullable String shield,
            @Nullable String name,
            @Nullable String skinUsername) {
        return spawn(owner, type, durationSeconds, health, armor, weapon, tool, shield, name, skinUsername, null);
    }

    @Nullable
    public static FabricCompanionEntity spawn(
            ServerPlayer owner,
            String type,
            int durationSeconds,
            @Nullable Float health,
            @Nullable String armor,
            @Nullable String weapon,
            @Nullable String tool,
            @Nullable String shield,
            @Nullable String name,
            @Nullable String skinUsername,
            @Nullable String mode) {
        CompanionCciSummonSupport.TypeSpec spec = CompanionCciSummonSupport.resolveType(type);
        FabricCompanionEntity companion = FabricCompanionRecruitment.spawnCciSummon(owner, spec.definitionId(AzsCompanionsFabric.MOD_ID));
        if (companion == null) {
            return null;
        }
        companion.setForm(CompanionForm.byName(spec.formName()));
        companion.setBodyScale(spec.bodyScale());
        companion.setAttitude(CompanionAttitude.PASSIVE);
        companion.setFightSpawn(true);
        companion.markCciSummoned(CompanionCciSummonSupport.expireAtGameTime(
                owner.level().getGameTime(), durationSeconds));
        companion.setMode(FabricCompanionMode.byName(CompanionCciSummonSupport.resolveBehaviorMode(mode)));

        String display = CompanionCciSummonSupport.sanitizeDisplayName(name);
        if (display.isEmpty() && skinUsername != null) {
            display = CompanionCciSummonSupport.sanitizeDisplayName(skinUsername);
        }
        if (!display.isEmpty()) {
            companion.setCustomDisplayName(display);
        }
        companion.setNameTagVisible(true);

        String skin = skinUsername != null && !skinUsername.isBlank() ? skinUsername : display;
        boolean appliedUsernameSkin = false;
        if (!skin.isBlank() && CompanionCciSummonSupport.wantsPlayerSkin(spec.formName())) {
            appliedUsernameSkin = resolveAndApplySkin(companion, skin);
        }
        if (CompanionCciSummonSupport.shouldApplyVanillaDefault(spec, appliedUsernameSkin)) {
            CompanionCciSummonSupport.VanillaPlayerSkin vanilla =
                    CompanionCciSummonSupport.pickVanillaPlayerSkin(spec.vanillaPlayerPick());
            if (vanilla != null) {
                companion.setSkinPath(vanilla.texturePath());
                companion.setSlimArms(vanilla.slim());
                if (display.isEmpty()) {
                    companion.setCustomDisplayName(vanilla.label());
                }
            }
        }

        if (health != null) {
            applyHealth(companion, health);
        }
        applyArmor(companion, armor);
        applySlot(companion, "mainhand", weapon);
        if (!CompanionCciSummonSupport.isSkipToken(tool)) {
            if (weapon == null || CompanionCciSummonSupport.isSkipToken(weapon)) {
                applySlot(companion, "mainhand", tool);
            } else {
                applySlot(companion, "tool", tool);
            }
        }
        applySlot(companion, "offhand", shield);
        return companion;
    }

    private static int killAll(CommandContext<CommandSourceStack> ctx, @Nullable ServerPlayer ownerFilter) {
        List<FabricCompanionEntity> targets = collectCciSummons(ctx.getSource().getServer().getAllLevels(), ownerFilter);
        if (targets.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable(CciMessages.CCI_KILL_NONE));
            return 0;
        }
        for (FabricCompanionEntity companion : targets) {
            killCciSummon(companion);
        }
        int count = targets.size();
        ctx.getSource().sendSuccess(() -> Component.translatable(CciMessages.CCI_KILL_ALL, count), true);
        return count;
    }

    private static int killNearest(CommandContext<CommandSourceStack> ctx) {
        Vec3 pos = ctx.getSource().getPosition();
        List<FabricCompanionEntity> targets = collectCciSummons(ctx.getSource().getServer().getAllLevels(), null);
        FabricCompanionEntity nearest = pickNearest(targets, pos);
        if (nearest == null) {
            ctx.getSource().sendFailure(Component.translatable(CciMessages.CCI_KILL_NONE));
            return 0;
        }
        String label = nearest.getChatDisplayName();
        killCciSummon(nearest);
        ctx.getSource().sendSuccess(() -> Component.translatable(CciMessages.CCI_KILL_ONE, label), true);
        return 1;
    }

    private static int killNamed(CommandContext<CommandSourceStack> ctx, String query) {
        Vec3 pos = ctx.getSource().getPosition();
        List<FabricCompanionEntity> matches = new ArrayList<>();
        for (FabricCompanionEntity companion : collectCciSummons(ctx.getSource().getServer().getAllLevels(), null)) {
            if (CompanionCciSummonSupport.displayNameMatches(companion.getChatDisplayName(), query)) {
                matches.add(companion);
            }
        }
        FabricCompanionEntity target = pickNearest(matches, pos);
        if (target == null) {
            ctx.getSource().sendFailure(Component.translatable(CciMessages.CCI_KILL_NONE_NAMED, query));
            return 0;
        }
        String label = target.getChatDisplayName();
        killCciSummon(target);
        ctx.getSource().sendSuccess(() -> Component.translatable(CciMessages.CCI_KILL_ONE, label), true);
        return 1;
    }

    private static List<FabricCompanionEntity> collectCciSummons(
            Iterable<ServerLevel> levels,
            @Nullable ServerPlayer ownerFilter) {
        List<FabricCompanionEntity> out = new ArrayList<>();
        UUID owner = ownerFilter == null ? null : ownerFilter.getUUID();
        for (ServerLevel dim : levels) {
            for (var entity : dim.getAllEntities()) {
                if (entity instanceof FabricCompanionEntity companion
                        && companion.isAlive()
                        && companion.isCciSummoned()
                        && (owner == null || owner.equals(companion.getOwnerUuid()))) {
                    out.add(companion);
                }
            }
        }
        return out;
    }

    @Nullable
    private static FabricCompanionEntity pickNearest(List<FabricCompanionEntity> candidates, Vec3 pos) {
        FabricCompanionEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (FabricCompanionEntity companion : candidates) {
            double d = companion.distanceToSqr(pos.x, pos.y, pos.z);
            if (d < bestD) {
                bestD = d;
                best = companion;
            }
        }
        return best;
    }

    private static void killCciSummon(FabricCompanionEntity companion) {
        companion.setInvulnerable(false);
        companion.hurt(companion.damageSources().genericKill(), Float.MAX_VALUE);
    }


    private static void applyHealth(FabricCompanionEntity companion, float health) {
        float clamped = CompanionCciSummonSupport.clampHealth(health);
        var attr = companion.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(clamped);
        }
        companion.setCciMaxHealth(clamped);
        companion.setHealth(clamped);
    }

    private static void applyArmor(FabricCompanionEntity companion, @Nullable String armor) {
        for (String id : CompanionCciSummonSupport.armorItemIds(armor)) {
            String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
            String slot = path.contains("helmet") ? "helmet"
                    : path.contains("chestplate") ? "chestplate"
                    : path.contains("leggings") ? "leggings"
                    : path.contains("boots") ? "boots"
                    : path.contains("wolf_armor") ? "chestplate"
                    : "chestplate";
            applySlot(companion, slot, id);
        }
    }

    private static void applySlot(FabricCompanionEntity companion, String slotKey, @Nullable String itemId) {
        if (CompanionCciSummonSupport.isSkipToken(itemId)) {
            return;
        }
        int invSlot = slotIndex(slotKey);
        if (invSlot < 0) {
            return;
        }
        Optional<ItemStack> parsed = parseItem(itemId);
        if (parsed.isEmpty()) {
            AzsCompanionsFabric.LOGGER.debug("CCI summon ignored invalid item {} for slot {}", itemId, slotKey);
            return;
        }
        ItemStack stack = parsed.get();
        if (FabricCompanionCharmItem.isCharm(stack)) {
            return;
        }
        EquipmentSlot eq = equipmentSlot(slotKey);
        if (!stack.isEmpty() && eq != null && eq.isArmor()
                && !CompanionArmorRules.mayPlaceInArmorSlot(companion.getForm(), eq, stack)) {
            return;
        }
        companion.getCompanionInventory().setItem(invSlot, stack);
        if (eq != null) {
            companion.setItemSlot(eq, stack.copy());
        }
    }

    private static int slotIndex(String key) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "mainhand", "main", "hand", "weapon" -> FabricCompanionInventory.MAIN_HAND;
            case "offhand", "off", "shield" -> FabricCompanionInventory.OFF_HAND;
            case "helmet", "head" -> FabricCompanionInventory.HEAD;
            case "chestplate", "chest" -> FabricCompanionInventory.CHEST;
            case "leggings", "legs" -> FabricCompanionInventory.LEGS;
            case "boots", "feet" -> FabricCompanionInventory.FEET;
            case "tool" -> FabricCompanionInventory.HOTBAR_EXTRA_START;
            default -> -1;
        };
    }

    @Nullable
    private static EquipmentSlot equipmentSlot(String key) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "mainhand", "main", "hand", "weapon" -> EquipmentSlot.MAINHAND;
            case "offhand", "off", "shield" -> EquipmentSlot.OFFHAND;
            case "helmet", "head" -> EquipmentSlot.HEAD;
            case "chestplate", "chest" -> EquipmentSlot.CHEST;
            case "leggings", "legs" -> EquipmentSlot.LEGS;
            case "boots", "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private static Optional<ItemStack> parseItem(String itemId) {
        String id = itemId.trim();
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        ResourceLocation loc = ResourceLocation.tryParse(id.toLowerCase(Locale.ROOT));
        if (loc == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(loc).map(ItemStack::new);
    }

    private static boolean resolveAndApplySkin(FabricCompanionEntity companion, String username) {
        try {
            String encoded = java.net.URLEncoder.encode(username.trim(), java.nio.charset.StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + encoded))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                AzsCompanionsFabric.LOGGER.info("CCI summon skin lookup: no Mojang profile for {}", username);
                return false;
            }
            String body = response.body();
            int idIdx = body.indexOf("\"id\"");
            if (idIdx < 0) {
                return false;
            }
            int q1 = body.indexOf('"', idIdx + 4);
            int q2 = body.indexOf('"', q1 + 1);
            if (q1 < 0 || q2 < 0) {
                return false;
            }
            String hex = body.substring(q1 + 1, q2).replace("-", "");
            if (hex.length() != 32) {
                return false;
            }
            UUID uuid = UUID.fromString(hex.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"));
            companion.setSkinPath("player:" + uuid);
            return true;
        } catch (Exception e) {
            AzsCompanionsFabric.LOGGER.warn("CCI summon Mojang skin lookup failed for {}", username, e);
            return false;
        }
    }
}
