package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.CompanionAiAsk;
import com.azscompanions.ai.CompanionAiChatSupport;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.cci.CciCompanionParams;
import com.azscompanions.cci.CciMessages;
import com.azscompanions.entity.CompanionAttitude;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.entity.CompanionRecruitment;
import com.azscompanions.entity.CompanionRegistry;
import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.item.CompanionCharmItem;
import com.azscompanions.util.CompanionArmorRules;
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
        if (!com.azscompanions.compat.ftb.FtbCompat.mayCci(player)) {
            toast(player, CciMsg.title(CciMessages.TITLE_BLOCKED), CciMsg.t(CciMessages.BLOCKED));
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

        if (TeamFightCciHandler.handle(player, action, message)) {
            return;
        }

        if (action == CciCompanionAction.AI_STATUS) {
            toast(player, CciMsg.title(CciMessages.TITLE_AI), Component.literal(CompanionAiAsk.status()));
            return;
        }
        if (action == CciCompanionAction.AI_CONFIG) {
            applyAiConfig(player, params);
            return;
        }

        CompanionEntity companion = findOwnedCompanion(player);
        if (companion == null) {
            AzsCompanions.LOGGER.debug("CCI action {} — no owned companion near {}", action, player.getGameProfile().getName());
            toast(player, CciMsg.title(CciMessages.TITLE_NO_COMPANION), CciMsg.t(CciMessages.NO_COMPANION));
            return;
        }

        switch (action) {
            case SAY -> sayOrAi(player, companion, params,
                    safe.isEmpty() ? CciMsg.plain(CciMessages.DIALOGUE_HELLO) : safe, false);
            case GREET -> {
                String canned = safe.isEmpty()
                        ? CciMsg.plain(CciMessages.DIALOGUE_GREET)
                        : CciMsg.plain(CciMessages.DIALOGUE_GREET_NAMED, safe);
                String prompt = safe.isEmpty()
                        ? "[cci greet] Thank a supporter warmly in character (1 short line)."
                        : "[cci greet] Thank supporter " + safe + " warmly in character (1 short line).";
                sayOrAi(player, companion, params, canned, true, prompt);
            }
            case WAVE -> {
                String canned = safe.isEmpty()
                        ? CciMsg.plain(CciMessages.DIALOGUE_WAVE)
                        : CciMsg.plain(CciMessages.DIALOGUE_WAVE_NAMED, safe);
                String prompt = safe.isEmpty()
                        ? "[cci wave] Wave hello in character (1 short line)."
                        : "[cci wave] Wave hello to " + safe + " in character (1 short line).";
                sayOrAi(player, companion, params, canned, true, prompt);
            }
            case FOLLOW -> {
                companion.setMode(CompanionMode.FOLLOW);
                companion.getTaskQueue().clear();
                toastNamed(player, companion, CciMsg.t(CciMessages.MODE_FOLLOW));
            }
            case SIT -> {
                companion.setMode(CompanionMode.SIT);
                toastNamed(player, companion, CciMsg.t(CciMessages.MODE_SIT));
            }
            case STAY -> {
                companion.setMode(CompanionMode.STAY);
                toastNamed(player, companion, CciMsg.t(CciMessages.MODE_STAY));
            }
            case SET_ATTITUDE -> {
                CompanionAttitude attitude = params.attitudeOr(CompanionAttitude.byName(params.getOr("raw", safe)));
                companion.setAttitude(attitude);
                toastNamed(player, companion, CciMsg.t(CciMessages.ATTITUDE, attitude.serializedName()));
            }
            case SET_TEAM -> {
                String team = params.teamOr(params.getOr("raw", safe));
                companion.setTeamId(team);
                toastNamed(player, companion, team.isBlank()
                        ? CciMsg.t(CciMessages.TEAM_CLEARED)
                        : CciMsg.t(CciMessages.TEAM, team));
            }
            case SET_MAINHAND -> applySingleSlot(player, companion, "mainhand",
                    params.first("mainhand", "main", "hand", "item", "raw"));
            case SET_OFFHAND -> applySingleSlot(player, companion, "offhand",
                    params.first("offhand", "off", "item", "raw"));
            case SET_ARMOR, SET_EQUIPMENT -> applyEquipmentParams(player, companion, params, safe);
            case MODIFY -> modify(player, companion, params);
            case PERSONA -> applyPersona(player, companion, params, true);
            case PLAY -> play(player, companion, params, null);
            case RUSH -> play(player, companion, params, "rush");
            case HIDE_SEEK -> play(player, companion, params, "hide_seek");
            case CLAIM_CHUNK -> claimOrUnclaim(player, companion, params, true);
            case UNCLAIM_CHUNK -> claimOrUnclaim(player, companion, params, false);
            case TURN_EVIL -> {
                int seconds = params.durationSecondsOr(CompanionEntity.PLAYFUL_EVIL_DEFAULT_SECONDS);
                companion.activatePlayfulEvil(seconds * 20);
                toastNamed(player, companion, CciMsg.t(CciMessages.TURN_EVIL, seconds));
            }
            case ASK -> askAi(player, companion, params, safe);
            case AI_CHAT -> aiChat(player, companion, params, safe);
            case TASK -> {
                String item = params.first("item", "block", "material", "id");
                int count = 64;
                String countRaw = params.first("count", "n", "num", "amount");
                if (countRaw != null && !countRaw.isBlank()) {
                    try {
                        count = Math.max(1, Math.min(1_000_000, Integer.parseInt(countRaw.trim())));
                    } catch (NumberFormatException ignored) {
                        count = 64;
                    }
                }
                String deposit = params.first("deposit", "chest", "to");
                if (deposit == null || deposit.isBlank()) {
                    deposit = "nearest";
                }
                if (item == null || item.isBlank()) {
                    toastNamed(player, companion, CciMsg.t(CciMessages.TASK_NEEDS_ITEM));
                } else {
                    com.azscompanions.task.CollectMaterialAssign.assign(
                            player, companion, item, count, deposit);
                }
            }
            default -> {
            }
        }
    }

    private static void sayOrAi(ServerPlayer player, CompanionEntity companion,
                                CciCompanionParams params, String canned, boolean preferAi) {
        sayOrAi(player, companion, params, canned, preferAi, "[cci say] " + canned);
    }

    private static void sayOrAi(ServerPlayer player, CompanionEntity companion,
                                CciCompanionParams params, String canned, boolean preferAi, String aiPrompt) {
        boolean forceAi = params.flag("ai", false) || params.flag("use_ai", false);
        boolean useAi = CompanionAiRuntime.get().isEnabled() && (preferAi || forceAi);
        if (useAi) {
            boolean ok = CompanionAiAsk.askQuiet(player, companion, player.getGameProfile().getName(), aiPrompt);
            toastNamed(player, companion, ok ? CciMsg.t(CciMessages.AI_PENDING) : Component.literal(canned));
            if (!ok) {
                say(player, companion, canned);
            }
            return;
        }
        say(player, companion, canned);
    }

    private static void askAi(ServerPlayer player, CompanionEntity companion, CciCompanionParams params, String safe) {
        if (!CompanionAiRuntime.get().isEnabled()) {
            toast(player, CciMsg.title(CciMessages.TITLE_AI),
                    CciMsg.t(CciMessages.AI_DISABLED, "config/azscompanions-ai.toml"));
            return;
        }
        String msg = params.first("message", "prompt", "text", "ask", "raw");
        if (msg == null || msg.isBlank()) {
            msg = safe;
        }
        if (msg == null || msg.isBlank()) {
            toastNamed(player, companion, CciMsg.t(CciMessages.ASK_NEEDS_MESSAGE));
            return;
        }
        int ok = CompanionAiAsk.ask(player, companion, msg, false, true);
        toastNamed(player, companion, ok > 0
                ? CciMsg.t(CciMessages.THINKING)
                : CciMsg.t(CciMessages.AI_BUSY_OR_FAILED));
    }

    private static void aiChat(ServerPlayer player, CompanionEntity companion, CciCompanionParams params, String safe) {
        if (!CompanionAiRuntime.get().isEnabled()) {
            toast(player, CciMsg.title(CciMessages.TITLE_AI),
                    CciMsg.t(CciMessages.AI_DISABLED, "config/azscompanions-ai.toml"));
            return;
        }
        String speaker = params.getOr("speaker", params.getOr("name", player.getGameProfile().getName()));
        String text = params.first("message", "text", "chat", "raw");
        if (text == null || text.isBlank()) {
            text = safe;
        }
        if (CompanionAiChatSupport.shouldIgnoreChatMessage(text)
                || CompanionAiChatSupport.looksLikeCompanionReply(text)) {
            return;
        }
        String prompt = CompanionAiChatSupport.chatReactionPrompt(speaker, text.trim());
        boolean ok = CompanionAiAsk.askQuiet(player, companion, speaker, prompt);
        if (ok) {
            CompanionAiRuntime.get().markChatReact(companion.getUUID());
        }
        toastNamed(player, companion, ok
                ? CciMsg.t(CciMessages.REACTING)
                : CciMsg.t(CciMessages.AI_BUSY));
    }

    private static void summon(ServerPlayer player, CciCompanionParams params, CompanionAttitude attitude) {
        CompanionEntity companion = CompanionRecruitment.recruit(player, CompanionRegistry.KON_ID.toString());
        if (companion == null) {
            toast(player, CciMsg.title(CciMessages.TITLE_SUMMON_FAILED), CciMsg.t(CciMessages.SUMMON_FAILED));
            return;
        }
        applyAppearance(companion, params, attitude, true);
        applyEquipmentParams(player, companion, params, null);
        boolean personaSet = applyPersona(player, companion, params, false);
        CompanionForm form = companion.getForm();
        String team = companion.getTeamId();
        Component body;
        if (team == null || team.isBlank()) {
            body = CciMsg.t(CciMessages.SUMMON_OK, form.displayLabel(),
                    attitude.serializedName().toLowerCase(Locale.ROOT));
        } else {
            body = CciMsg.t(CciMessages.SUMMON_OK_TEAM, form.displayLabel(),
                    attitude.serializedName().toLowerCase(Locale.ROOT), team);
        }
        if (personaSet) {
            body = body.copy().append(CciMsg.t(CciMessages.PERSONA_SET_SUFFIX));
        }
        toastNamed(player, companion, body);
        if (!personaSet) {
            com.azscompanions.ai.CompanionPersonaOnboarding.offerIfNeeded(player, companion);
        }
    }

    /**
     * Customize the owner's currently called / summoned companion in place
     * (form, skin, name, attitude, team, equipment, persona) — does not recruit a new one.
     */
    private static void modify(ServerPlayer player, CompanionEntity companion, CciCompanionParams params) {
        CompanionAttitude attitude = params.has("attitude") || params.has("stance") || params.has("mode")
                ? params.attitudeOr(companion.getAttitude())
                : companion.getAttitude();
        boolean changedAppearance = applyAppearance(companion, params, attitude, false);
        boolean hadEquipmentKeys = params.has("mainhand") || params.has("main") || params.has("hand")
                || params.has("offhand") || params.has("off")
                || params.has("helmet") || params.has("head")
                || params.has("chestplate") || params.has("chest")
                || params.has("leggings") || params.has("legs")
                || params.has("boots") || params.has("feet");
        if (hadEquipmentKeys) {
            applyEquipmentParams(player, companion, params, null);
        }
        boolean personaSet = applyPersona(player, companion, params, false);
        if (changedAppearance || hadEquipmentKeys || personaSet) {
            toastNamed(player, companion, personaSet
                    ? CciMsg.t(CciMessages.MODIFY_OK_PERSONA,
                    companion.getForm().displayLabel(),
                    companion.getAttitude().serializedName().toLowerCase(Locale.ROOT))
                    : CciMsg.t(CciMessages.MODIFY_OK,
                    companion.getForm().displayLabel(),
                    companion.getAttitude().serializedName().toLowerCase(Locale.ROOT)));
        } else {
            toastNamed(player, companion, CciMsg.t(CciMessages.MODIFY_NOTHING));
        }
    }

    /**
     * Apply CCI whoAmI/whatAmIDoing/howWillIBe (and aliases). Marks {@code personaInitialized}.
     * Supports {@code op=get|clear} on {@code companion_persona}.
     * @param toastAlways when true (companion_persona), always toast even if no keys.
     * @return true when persona keys were applied / cleared
     */
    private static boolean applyPersona(ServerPlayer player, CompanionEntity companion,
                                        CciCompanionParams params, boolean toastAlways) {
        if (toastAlways && params.wantsPersonaGet() && !com.azscompanions.ai.CompanionPersona.hasPersonaKeys(params)) {
            String summary = companion.getPersona().formatSummary(companion.getChatDisplayName());
            player.sendSystemMessage(Component.literal(summary));
            toastNamed(player, companion, CciMsg.t(CciMessages.PERSONA_GET));
            return false;
        }
        if (toastAlways && params.wantsPersonaClear()) {
            companion.setPersona(com.azscompanions.ai.CompanionPersona.EMPTY.cleared());
            toastNamed(player, companion, CciMsg.t(CciMessages.PERSONA_CLEARED));
            return true;
        }
        if (!com.azscompanions.ai.CompanionPersona.hasPersonaKeys(params)) {
            if (toastAlways) {
                toastNamed(player, companion, CciMsg.t(CciMessages.PERSONA_NEEDS));
            }
            return false;
        }
        var merged = companion.getPersona().mergeFromCci(params);
        companion.setPersona(merged);
        if (toastAlways) {
            toastNamed(player, companion, CciMsg.t(CciMessages.PERSONA_UPDATED));
        }
        return true;
    }

    private static void play(ServerPlayer player, CompanionEntity companion,
                             CciCompanionParams params, @Nullable String defaultMode) {
        String mode = params.playModeOr(defaultMode == null ? "" : defaultMode);
        if (mode.isBlank()) {
            mode = params.getOr("raw", defaultMode == null ? "rush" : defaultMode);
        }
        String key = mode == null ? "rush" : mode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        int seconds = params.playSecondsOr(switch (key) {
            case "hide", "hider", "hide_seek", "hideandseek", "hide_and_seek" -> 10;
            case "seek", "seeker" -> 15;
            case "dance", "spin" -> 4;
            case "peekaboo", "peek" -> 3;
            default -> 5;
        });
        int ticks = seconds * 20;
        switch (key) {
            case "stop", "clear", "none", "off" -> {
                companion.clearPlayMode();
                toastNamed(player, companion, CciMsg.t(CciMessages.PLAY_STOP));
            }
            case "rush", "run", "run_at_player", "charge" -> {
                companion.startPlay(com.azscompanions.entity.CompanionPlayMode.RUN_AT_PLAYER, ticks);
                companion.setMode(CompanionMode.FOLLOW);
                toastNamed(player, companion, CciMsg.t(CciMessages.PLAY_RUSH));
            }
            case "hide", "hider" -> {
                companion.startPlay(com.azscompanions.entity.CompanionPlayMode.HIDE, ticks);
                toastNamed(player, companion, CciMsg.t(CciMessages.PLAY_HIDE));
            }
            case "seek", "seeker" -> {
                companion.startPlay(com.azscompanions.entity.CompanionPlayMode.SEEK, ticks);
                toastNamed(player, companion, CciMsg.t(CciMessages.PLAY_SEEK));
            }
            case "hide_seek", "hideandseek", "hide_and_seek" -> {
                String role = params.playRoleOr("hider");
                if (role.equalsIgnoreCase("seek") || role.equalsIgnoreCase("seeker")) {
                    companion.startPlay(com.azscompanions.entity.CompanionPlayMode.SEEK, ticks);
                    toastNamed(player, companion, CciMsg.t(CciMessages.PLAY_HIDE_SEEK_SEEK));
                } else {
                    companion.startPlay(com.azscompanions.entity.CompanionPlayMode.HIDE, ticks);
                    toastNamed(player, companion, CciMsg.t(CciMessages.PLAY_HIDE_SEEK_HIDE));
                }
            }
            case "dance", "spin" -> {
                companion.startPlay(com.azscompanions.entity.CompanionPlayMode.DANCE, ticks);
                toastNamed(player, companion, CciMsg.t(CciMessages.PLAY_DANCE));
            }
            case "peekaboo", "peek" -> {
                companion.startPlay(com.azscompanions.entity.CompanionPlayMode.PEEKABOO, ticks);
                toastNamed(player, companion, CciMsg.t(CciMessages.PLAY_PEEKABOO));
            }
            default -> toastNamed(player, companion, CciMsg.t(CciMessages.PLAY_UNKNOWN));
        }
    }

    private static void claimOrUnclaim(ServerPlayer player, CompanionEntity companion,
                                       CciCompanionParams params, boolean claim) {
        if (!(companion.level() instanceof ServerLevel level)) {
            return;
        }
        if (!com.azscompanions.compat.ftb.FtbCompat.aiClaimEnabled()) {
            toast(player, CciMsg.title(CciMessages.TITLE_FTB), CciMsg.t(CciMessages.FTB_UNAVAILABLE));
            return;
        }
        if (!com.azscompanions.compat.ftb.FtbCompat.mayAiActions(player)) {
            toast(player, CciMsg.title(CciMessages.TITLE_FTB), CciMsg.t(CciMessages.FTB_BLOCKED));
            return;
        }
        int footX = companion.blockPosition().getX() >> 4;
        int footZ = companion.blockPosition().getZ() >> 4;
        int cx = params.chunkXOr(footX);
        int cz = params.chunkZOr(footZ);
        String result = claim
                ? com.azscompanions.compat.ftb.FtbCompat.claimChunkAsOwner(player, level.dimension(), cx, cz)
                : com.azscompanions.compat.ftb.FtbCompat.unclaimChunkAsOwner(player, level.dimension(), cx, cz);
        toastNamed(player, companion, claim
                ? CciMsg.t(CciMessages.CLAIM_RESULT, cx, cz, result)
                : CciMsg.t(CciMessages.UNCLAIM_RESULT, cx, cz, result));
    }

    private static void applyAiConfig(ServerPlayer player, CciCompanionParams params) {
        toast(player, CciMsg.title(CciMessages.TITLE_AI),
                CciMsg.t(CciMessages.AI_CONFIG_STATUS, CompanionAiAsk.status()));
    }

    private static boolean applyAppearance(CompanionEntity companion, CciCompanionParams params,
                                           CompanionAttitude attitude, boolean forceAttitude) {
        boolean changed = false;
        boolean hasForm = params.has("form") || params.has("mob") || params.has("species");
        CompanionForm form = hasForm || forceAttitude
                ? params.formOr(forceAttitude ? CompanionForm.PLAYER : companion.getForm())
                : companion.getForm();
        if (hasForm || forceAttitude) {
            companion.setForm(form);
            changed = true;
        }
        if (forceAttitude || params.has("attitude") || params.has("stance") || params.has("mode")) {
            companion.setAttitude(attitude);
            changed = true;
        }
        if (params.has("team") || params.has("teamid") || params.has("squad")) {
            String team = params.teamOr("");
            companion.setTeamId(team);
            changed = true;
        }
        String name = params.displayName();
        if (name != null && !name.isBlank() && (params.has("name") || params.has("displayname"))) {
            companion.setCustomDisplayName(name);
            changed = true;
        }
        String skinUser = params.skinUsername();
        if (skinUser != null && !skinUser.isBlank()) {
            if (form.isPlayer() || hasForm) {
                if (!form.isPlayer()) {
                    companion.setForm(CompanionForm.PLAYER);
                    form = CompanionForm.PLAYER;
                }
                resolveAndApplySkin(companion, skinUser);
                if ((!params.has("name") && !params.has("displayname"))
                        && (name == null || name.isBlank())) {
                    companion.setCustomDisplayName(skinUser);
                }
                changed = true;
            }
        } else if (hasForm && !form.isPlayer()) {
            companion.setSkinPath("");
            changed = true;
        }
        Boolean showArmor = params.showArmorOrNull();
        if (showArmor != null) {
            companion.setArmorVisible(showArmor);
            changed = true;
        }
        Float followRadius = params.followRadiusOrNull();
        if (followRadius != null) {
            companion.setFollowRadius(followRadius);
            changed = true;
        }
        Float personalSpace = params.personalSpaceOrNull();
        if (personalSpace != null) {
            companion.setPersonalSpace(personalSpace);
            changed = true;
        }
        Float wanderRadius = params.wanderRadiusOrNull();
        if (wanderRadius != null) {
            companion.setWanderRadius(wanderRadius);
            changed = true;
        }
        Integer maxChildren = params.maxChildrenOrNull();
        if (maxChildren != null) {
            companion.setMaxChildren(maxChildren);
            changed = true;
        }
        Boolean chunkLoading = params.chunkLoadingOrNull();
        if (chunkLoading != null) {
            companion.setChunkLoadingEnabled(chunkLoading);
            changed = true;
        }
        return changed;
    }

    private static void applySingleSlot(ServerPlayer player, CompanionEntity companion, String slotKey, @Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) {
            toastNamed(player, companion, CciMsg.t(CciMessages.EQUIP_NO_ID, slotKey));
            return;
        }
        if (setEquipmentSlot(companion, slotKey, itemId)) {
            toastNamed(player, companion, CciMsg.t(CciMessages.EQUIP_SET, slotKey, itemId));
        } else {
            toastNamed(player, companion, CciMsg.t(CciMessages.EQUIP_INVALID, itemId));
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
            toastNamed(player, companion, CciMsg.t(CciMessages.EQUIP_UPDATED));
        } else if (fallbackRaw != null) {
            toastNamed(player, companion, CciMsg.t(CciMessages.EQUIP_NONE));
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
        EquipmentSlot eq = equipmentSlot(slotKey);
        if (CompanionCharmItem.isCharm(stack)) {
            return false;
        }
        if (!stack.isEmpty() && eq != null && eq.isArmor()
                && !CompanionArmorRules.mayPlaceInArmorSlot(companion.getForm(), eq, stack)) {
            return false;
        }
        companion.getCompanionInventory().setStackInSlot(invSlot, stack);
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
                CciMsg.t(CciMessages.SAY_FORMAT, companion.getChatDisplayName(), line),
                false);
        toastNamed(owner, companion, Component.literal(line));
    }

    private static void toastNamed(ServerPlayer player, CompanionEntity companion, Component body) {
        toast(player, CciMsg.named(companion.getChatDisplayName()), body);
    }

    private static void toast(ServerPlayer player, Component title, Component body) {
        try {
            IApi api = CCIApi.getApiImpl();
            if (api != null) {
                api.triggerInformationalToast(title, body);
            }
        } catch (Throwable t) {
            AzsCompanions.LOGGER.debug("CCI toast unavailable: {}", t.toString());
        }
        player.displayClientMessage(CciMsg.actionBar(title, body), true);
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
                c -> c.isAlive() && c.isOwnedBy(player));
        return found.stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(player)))
                .orElse(null);
    }
}
