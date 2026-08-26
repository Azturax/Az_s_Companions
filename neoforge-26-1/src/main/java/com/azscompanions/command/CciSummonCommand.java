package com.azscompanions.command;

import com.azscompanions.AzsCompanions;
import com.azscompanions.cci.CciCompanionParams;
import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionAttitude;
import com.azscompanions.entity.CompanionCciSummonSupport;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionRecruitment;
import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.item.CompanionCharmItem;
import com.azscompanions.util.CompanionArmorRules;
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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * CCI / streamer temporary summon.
 * <p>
 * Syntax (permission 2):
 * {@code /az summon <type> [player] [durationSeconds] [health] [armor] [weapon] [tool] [shield] [name]}
 * <p>
 * Use {@code -} to skip an optional equipment/health token. Duration {@code 0} disables expiry
 * (testing). Default duration is {@code cciSummonDurationSeconds} (90). Charm companions are never
 * spawned or expired by this command.
 */
public final class CciSummonCommand {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private CciSummonCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildBranch() {
        var nameArg = Commands.argument("name", StringArgumentType.greedyString())
                .executes(ctx -> equipped(ctx, StringArgumentType.getString(ctx, "name")));
        var shieldArg = Commands.argument("shield", StringArgumentType.word())
                .executes(ctx -> equipped(ctx, null))
                .then(nameArg);
        var toolArg = Commands.argument("tool", StringArgumentType.word())
                .executes(ctx -> run(ctx, player(ctx), dur(ctx), hp(ctx), armor(ctx), weapon(ctx),
                        StringArgumentType.getString(ctx, "tool"), null, null))
                .then(shieldArg);
        var weaponArg = Commands.argument("weapon", StringArgumentType.word())
                .executes(ctx -> run(ctx, player(ctx), dur(ctx), hp(ctx), armor(ctx),
                        StringArgumentType.getString(ctx, "weapon"), null, null, null))
                .then(toolArg);
        var armorArg = Commands.argument("armor", StringArgumentType.word())
                .executes(ctx -> run(ctx, player(ctx), dur(ctx), hp(ctx),
                        StringArgumentType.getString(ctx, "armor"), null, null, null, null))
                .then(weaponArg);
        var healthArg = Commands.argument("health", IntegerArgumentType.integer(1, (int) CompanionCciSummonSupport.MAX_HEALTH_VALUE))
                .executes(ctx -> run(ctx, player(ctx), dur(ctx), hp(ctx), null, null, null, null, null))
                .then(armorArg);
        var durationArg = Commands.argument("durationSeconds", IntegerArgumentType.integer(0, CompanionCciSummonSupport.MAX_DURATION_SECONDS))
                .executes(ctx -> run(ctx, player(ctx), dur(ctx), -1, null, null, null, null, null))
                .then(healthArg);
        var playerArg = Commands.argument("player", EntityArgument.player())
                .executes(ctx -> run(ctx, player(ctx), defaultDuration(), -1, null, null, null, null, null))
                .then(durationArg);
        return Commands.literal("summon")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("type", StringArgumentType.word())
                        .executes(ctx -> run(ctx, null, defaultDuration(), -1, null, null, null, null, null))
                        .then(playerArg));
    }

    private static int equipped(CommandContext<CommandSourceStack> ctx, @Nullable String name) throws CommandSyntaxException {
        return run(ctx, player(ctx), dur(ctx), hp(ctx), armor(ctx), weapon(ctx),
                StringArgumentType.getString(ctx, "tool"),
                StringArgumentType.getString(ctx, "shield"),
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
        return ServerConfig.CCI_SUMMON_DURATION_SECONDS.get();
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
            @Nullable String name) throws CommandSyntaxException {
        ServerPlayer owner = target != null ? target : ctx.getSource().getPlayerOrException();
        String type = StringArgumentType.getString(ctx, "type");
        CompanionEntity spawned = spawn(
                owner,
                type,
                durationSeconds,
                health > 0 ? (float) health : null,
                armor,
                weapon,
                tool,
                shield,
                name,
                null);
        if (spawned == null) {
            ctx.getSource().sendFailure(Component.translatable("message.azscompanions.cci.cci_summon_failed"));
            return 0;
        }
        int shownDuration = CompanionCciSummonSupport.clampDurationSeconds(durationSeconds);
        String window = shownDuration <= 0 ? "no expiry" : shownDuration + "s";
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "message.azscompanions.cci.cci_summon_ok",
                spawned.getChatDisplayName(),
                owner.getGameProfile().name(),
                window), true);
        return 1;
    }

    /**
     * CCI IMC / {@code /azscci companion_cci_summon} entry. Always owned by the streamer player.
     */
    @Nullable
    public static CompanionEntity spawnFromCci(ServerPlayer streamer, CciCompanionParams params) {
        if (streamer == null) {
            return null;
        }
        int duration = params.summonDurationSecondsOr(defaultDuration());
        return spawn(
                streamer,
                params.companionTypeOr("kon"),
                duration,
                params.healthOrNull(),
                params.armorSpec(),
                params.weaponItem(),
                params.toolItem(),
                params.shieldItem(),
                params.summonDisplayName(),
                params.skinUsername());
    }

    @Nullable
    public static CompanionEntity spawn(
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
        CompanionCciSummonSupport.TypeSpec spec = CompanionCciSummonSupport.resolveType(type);
        CompanionEntity companion = CompanionRecruitment.spawnCciSummon(owner, spec.definitionId(AzsCompanions.MOD_ID));
        if (companion == null) {
            return null;
        }
        companion.setForm(CompanionForm.byName(spec.formName()));
        companion.setBodyScale(spec.bodyScale());
        companion.setAttitude(CompanionAttitude.PASSIVE);
        companion.setFightSpawn(true);
        companion.markCciSummoned(CompanionCciSummonSupport.expireAtGameTime(
                owner.level().getGameTime(), durationSeconds));

        String display = CompanionCciSummonSupport.sanitizeDisplayName(name);
        if (display.isEmpty() && skinUsername != null) {
            display = CompanionCciSummonSupport.sanitizeDisplayName(skinUsername);
        }
        if (!display.isEmpty()) {
            companion.setCustomDisplayName(display);
        }
        companion.setNameTagVisible(true);

        String skin = skinUsername != null && !skinUsername.isBlank() ? skinUsername : display;
        if (!skin.isBlank() && CompanionCciSummonSupport.wantsPlayerSkin(spec.formName())) {
            resolveAndApplySkin(companion, skin);
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

    private static void applyHealth(CompanionEntity companion, float health) {
        float clamped = CompanionCciSummonSupport.clampHealth(health);
        var attr = companion.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(clamped);
        }
        companion.setCciMaxHealth(clamped);
        companion.setHealth(clamped);
    }

    private static void applyArmor(CompanionEntity companion, @Nullable String armor) {
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

    private static void applySlot(CompanionEntity companion, String slotKey, @Nullable String itemId) {
        if (CompanionCciSummonSupport.isSkipToken(itemId)) {
            return;
        }
        int invSlot = slotIndex(slotKey);
        if (invSlot < 0) {
            return;
        }
        Optional<ItemStack> parsed = parseItem(itemId);
        if (parsed.isEmpty()) {
            AzsCompanions.LOGGER.debug("CCI summon ignored invalid item {} for slot {}", itemId, slotKey);
            return;
        }
        ItemStack stack = parsed.get();
        if (CompanionCharmItem.isCharm(stack)) {
            return;
        }
        EquipmentSlot eq = equipmentSlot(slotKey);
        if (!stack.isEmpty() && eq != null && eq.isArmor()
                && !CompanionArmorRules.mayPlaceInArmorSlot(companion.getForm(), eq, stack)) {
            return;
        }
        companion.getCompanionInventory().setStackInSlot(invSlot, stack);
        if (eq != null) {
            companion.setItemSlot(eq, stack.copy());
        }
    }

    private static int slotIndex(String key) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "mainhand", "main", "hand", "weapon" -> CompanionInventory.MAIN_HAND;
            case "offhand", "off", "shield" -> CompanionInventory.OFF_HAND;
            case "helmet", "head" -> CompanionInventory.HEAD;
            case "chestplate", "chest" -> CompanionInventory.CHEST;
            case "leggings", "legs" -> CompanionInventory.LEGS;
            case "boots", "feet" -> CompanionInventory.FEET;
            case "tool" -> CompanionInventory.HOTBAR_EXTRA_START;
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
        Identifier loc = Identifier.tryParse(id.toLowerCase(Locale.ROOT));
        if (loc == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(loc).map(ItemStack::new);
    }

    private static void resolveAndApplySkin(CompanionEntity companion, String username) {
        try {
            String encoded = java.net.URLEncoder.encode(username.trim(), java.nio.charset.StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + encoded))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                AzsCompanions.LOGGER.info("CCI summon skin lookup: no Mojang profile for {}", username);
                return;
            }
            String body = response.body();
            int idIdx = body.indexOf("\"id\"");
            if (idIdx < 0) {
                return;
            }
            int q1 = body.indexOf('"', idIdx + 4);
            int q2 = body.indexOf('"', q1 + 1);
            if (q1 < 0 || q2 < 0) {
                return;
            }
            String hex = body.substring(q1 + 1, q2).replace("-", "");
            if (hex.length() != 32) {
                return;
            }
            UUID uuid = UUID.fromString(hex.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"));
            companion.setSkinPath("player:" + uuid);
        } catch (Exception e) {
            AzsCompanions.LOGGER.warn("CCI summon Mojang skin lookup failed for {}", username, e);
        }
    }
}
