package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanions;
import com.azscompanions.cci.CciCompanionParams;
import com.azscompanions.entity.CompanionAttitude;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.entity.CompanionRecruitment;
import com.azscompanions.entity.CompanionRegistry;
import com.azscompanions.entity.inventory.CompanionInventory;
import me.ichun.mods.cci.api.CCIApi;
import me.ichun.mods.cci.api.IApi;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies stream-driven companion behaviours for the CCI edition.
 */
public final class CciCompanionActions {
    private static final double SEARCH_RANGE = 96.0d;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private CciCompanionActions() {
    }

    public static void applyOnServer(@Nullable ServerPlayer player, CciCompanionAction action, String message) {
        if (player == null) {
            AzsCompanions.LOGGER.debug("CCI action {} ignored — no player context", action);
            return;
        }
        CciCompanionParams params = CciCompanionParams.parse(message);
        String safe = message == null ? "" : message.trim();

        if (action.isSummon()) {
            CompanionAttitude attitude = switch (action) {
                case SUMMON_HOSTILE -> CompanionAttitude.HOSTILE;
                case SUMMON_PASSIVE -> CompanionAttitude.PASSIVE;
                default -> params.attitudeOr(CompanionAttitude.PASSIVE);
            };
            summon(player, params, attitude);
            return;
        }

        CompanionEntity companion = findOwnedCompanion(player);
        if (companion == null) {
            AzsCompanions.LOGGER.debug("CCI action {} — no owned companion near {}", action, player.getGameProfile().getName());
            toast(player, "No companion nearby", "Summon your companion before using CCI outcomes.");
            return;
        }

        switch (action) {
            case SAY -> say(player, companion, safe.isEmpty() ? "Hello!" : safe);
            case GREET -> say(player, companion, safe.isEmpty()
                    ? "Thanks for the support!"
                    : "Thanks for the support, " + safe + "!");
            case WAVE -> say(player, companion, safe.isEmpty()
                    ? "Hello there!"
                    : "Hello, " + safe + "!");
            case FOLLOW -> {
                companion.setMode(CompanionMode.FOLLOW);
                companion.getTaskQueue().clear();
                toast(player, companion.getChatDisplayName(), "Following you.");
            }
            case SIT -> {
                companion.setMode(CompanionMode.SIT);
                toast(player, companion.getChatDisplayName(), "Sitting.");
            }
            case STAY -> {
                companion.setMode(CompanionMode.STAY);
                toast(player, companion.getChatDisplayName(), "Staying put.");
            }
            case SET_ATTITUDE -> {
                CompanionAttitude attitude = params.attitudeOr(CompanionAttitude.byName(params.getOr("raw", safe)));
                companion.setAttitude(attitude);
                toast(player, companion.getChatDisplayName(), "Attitude: " + attitude.serializedName());
            }
            case SET_TEAM -> {
                String team = params.teamOr(params.getOr("raw", safe));
                companion.setTeamId(team);
                toast(player, companion.getChatDisplayName(),
                        team.isBlank() ? "Team cleared." : "Team: " + team);
            }
            case SET_MAINHAND -> applySingleSlot(player, companion, "mainhand",
                    params.first("mainhand", "main", "hand", "item", "raw"));
            case SET_OFFHAND -> applySingleSlot(player, companion, "offhand",
                    params.first("offhand", "off", "item", "raw"));
            case SET_ARMOR, SET_EQUIPMENT -> applyEquipmentParams(player, companion, params, safe);
            default -> {
            }
        }
    }

    private static void summon(ServerPlayer player, CciCompanionParams params, CompanionAttitude attitude) {
        CompanionEntity companion = CompanionRecruitment.recruit(player, CompanionRegistry.KON_ID.toString());
        if (companion == null) {
            toast(player, "Summon failed", "Companion limit reached or spawn failed.");
            return;
        }
        CompanionForm form = params.formOr(CompanionForm.PLAYER);
        companion.setForm(form);
        companion.setAttitude(attitude);
        String team = params.teamOr("");
        if (!team.isBlank()) {
            companion.setTeamId(team);
        }
        String name = params.displayName();
        if (name != null && !name.isBlank()) {
            companion.setCustomDisplayName(name);
        }
        String skinUser = params.skinUsername();
        if (form.isPlayer() && skinUser != null && !skinUser.isBlank()) {
            resolveAndApplySkin(companion, skinUser);
            if (name == null || name.isBlank()) {
                companion.setCustomDisplayName(skinUser);
            }
        } else if (!form.isPlayer()) {
            companion.setSkinPath("");
        }
        applyEquipmentParams(player, companion, params, null);
        toast(player, companion.getChatDisplayName(),
                "Summoned " + form.displayLabel() + " (" + attitude.serializedName().toLowerCase(Locale.ROOT) + ")"
                        + (team.isBlank() ? "" : " team=" + team));
    }

    private static void applySingleSlot(ServerPlayer player, CompanionEntity companion, String slotKey, @Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) {
            toast(player, companion.getChatDisplayName(), "No item id for " + slotKey);
            return;
        }
        if (setEquipmentSlot(companion, slotKey, itemId)) {
            toast(player, companion.getChatDisplayName(), slotKey + " → " + itemId);
        } else {
            toast(player, companion.getChatDisplayName(), "Invalid item: " + itemId);
        }
    }

    private static void applyEquipmentParams(ServerPlayer player, CompanionEntity companion,
                                              CciCompanionParams params, @Nullable String fallbackRaw) {
        boolean any = false;
        String[] keys = {
                "mainhand", "main", "hand",
                "offhand", "off",
                "helmet", "head",
                "chestplate", "chest",
                "leggings", "legs",
                "boots", "feet"
        };
        for (String key : keys) {
            if (!params.has(key)) {
                continue;
            }
            if (setEquipmentSlot(companion, key, params.get(key))) {
                any = true;
            }
        }
        // Bare item id for SET_ARMOR / SET_EQUIPMENT without keys → mainhand
        if (!any && fallbackRaw != null && !fallbackRaw.isBlank() && !fallbackRaw.contains("=")) {
            any = setEquipmentSlot(companion, "mainhand", fallbackRaw);
        }
        if (any) {
            toast(player, companion.getChatDisplayName(), "Equipment updated.");
        } else if (fallbackRaw != null) {
            toast(player, companion.getChatDisplayName(), "No valid equipment in message.");
        }
    }

    private static boolean setEquipmentSlot(CompanionEntity companion, String slotKey, String itemId) {
        int invSlot = slotIndex(slotKey);
        if (invSlot < 0) {
            return false;
        }
        ItemStack stack;
        if (CciCompanionParams.isClearToken(itemId)) {
            stack = ItemStack.EMPTY;
        } else {
            Optional<ItemStack> parsed = parseItem(itemId);
            if (parsed.isEmpty()) {
                return false;
            }
            stack = parsed.get();
        }
        companion.getCompanionInventory().setStackInSlot(invSlot, stack);
        // Mirror onto living equipment so render/combat see it.
        EquipmentSlot eq = equipmentSlot(slotKey);
        if (eq != null) {
            companion.setItemSlot(eq, stack.copy());
        }
        return true;
    }

    private static int slotIndex(String key) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "mainhand", "main", "hand" -> CompanionInventory.MAIN_HAND;
            case "offhand", "off" -> CompanionInventory.OFF_HAND;
            case "helmet", "head" -> CompanionInventory.HEAD;
            case "chestplate", "chest" -> CompanionInventory.CHEST;
            case "leggings", "legs" -> CompanionInventory.LEGS;
            case "boots", "feet" -> CompanionInventory.FEET;
            default -> -1;
        };
    }

    @Nullable
    private static EquipmentSlot equipmentSlot(String key) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "mainhand", "main", "hand" -> EquipmentSlot.MAINHAND;
            case "offhand", "off" -> EquipmentSlot.OFFHAND;
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
        if (loc == null || !BuiltInRegistries.ITEM.containsKey(loc)) {
            return Optional.empty();
        }
        Item item = BuiltInRegistries.ITEM.get(loc);
        if (item == null || ItemStack.EMPTY.getItem() == item && !loc.getPath().equals("air")) {
            // BuiltInRegistries returns air for missing on some mappings — already checked containsKey.
        }
        return Optional.of(new ItemStack(item));
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
                AzsCompanions.LOGGER.info("CCI skin lookup: no Mojang profile for {}", username);
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
            AzsCompanions.LOGGER.warn("CCI Mojang skin lookup failed for {}", username, e);
        }
    }

    private static void say(ServerPlayer owner, CompanionEntity companion, String line) {
        owner.displayClientMessage(
                Component.literal("<" + companion.getChatDisplayName() + "> " + line),
                false);
        toast(owner, companion.getChatDisplayName(), line);
    }

    private static void toast(ServerPlayer player, String title, String body) {
        try {
            IApi api = CCIApi.getApiImpl();
            if (api != null) {
                api.triggerInformationalToast(Component.literal(title), Component.literal(body));
            }
        } catch (Throwable t) {
            AzsCompanions.LOGGER.debug("CCI toast unavailable: {}", t.toString());
        }
        player.displayClientMessage(Component.literal(title + " — " + body), true);
    }

    @Nullable
    private static CompanionEntity findOwnedCompanion(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        AABB box = player.getBoundingBox().inflate(SEARCH_RANGE);
        List<CompanionEntity> found = level.getEntitiesOfClass(
                CompanionEntity.class,
                box,
                c -> c.isAlive() && (c.isOwnedBy(player) || c.isTrusted(player)));
        return found.stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(player)))
                .orElse(null);
    }
}
