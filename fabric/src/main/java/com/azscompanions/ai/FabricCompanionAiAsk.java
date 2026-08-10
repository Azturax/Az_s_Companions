package com.azscompanions.ai;

import com.azscompanions.compat.ftb.FtbCompat;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.network.FabricNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Fabric helper: ask nearby owned companion via configured LLM / MCP provider.
 * Primary chat path is name-mention ({@code Kon, how are you?}) — slash ask is optional.
 */
public final class FabricCompanionAiAsk {
    private FabricCompanionAiAsk() {
    }

    public static int ask(ServerPlayer player, FabricCompanionEntity companion, String message) {
        return ask(player, companion, message, true, true);
    }

    /**
     * @param announceThinking show action-bar "thinking" (command use)
     * @param reportErrors show empty/error messages to the player (false for ambient/chat react)
     */
    public static int ask(ServerPlayer player, FabricCompanionEntity companion, String message,
                          boolean announceThinking, boolean reportErrors) {
        if (player == null || companion == null || companion.isRemoved() || !companion.isOwnedBy(player)) {
            if (reportErrors && player != null) {
                player.displayClientMessage(Component.literal(
                        "Ask only works on your own companions (multiplayer-safe)."), false);
            }
            return 0;
        }
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        if (!runtime.isEnabled()) {
            if (reportErrors) {
                player.displayClientMessage(Component.literal(
                        "Companion AI is disabled. Edit config/azscompanions-ai.json on the server (provider)."), false);
            }
            return 0;
        }
        if (!FtbCompat.mayAsk(player)) {
            if (reportErrors) {
                player.displayClientMessage(Component.literal(
                        "You lack permission to ask companions (FTB Ranks)."), false);
            }
            return 0;
        }
        String safeMessage = censorPrompt(message, runtime.settings(), true);
        CompanionChatContext ctx = buildContext(companion, player.getGameProfile().getName(), safeMessage, runtime);
        if (announceThinking) {
            player.displayClientMessage(Component.literal("… " + companion.getChatDisplayName() + " is thinking"), true);
        }
        notifyThinking(player, companion, true);
        boolean accepted = runtime.requestChatAsync(ctx, (reply, error) -> {
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            server.execute(() -> {
                notifyThinking(player, companion, false);
                deliver(player, companion, reply, error, reportErrors);
            });
        });
        if (!accepted) {
            notifyThinking(player, companion, false);
        }
        return accepted ? 1 : 0;
    }

    /**
     * Auto chat-react / ambient / call — quiet, uses speaker name in the prompt context as playerName.
     */
    public static boolean askQuiet(ServerPlayer owner, FabricCompanionEntity companion,
                                   String speakerName, String promptMessage) {
        return askQuiet(owner, companion, speakerName, promptMessage, true, null);
    }

    public static boolean askQuiet(ServerPlayer owner, FabricCompanionEntity companion,
                                   String speakerName, String promptMessage, boolean allowActions) {
        return askQuiet(owner, companion, speakerName, promptMessage,
                allowActions ? CompanionAiActionTrust.OWNER : CompanionAiActionTrust.NONE, null);
    }

    /**
     * Quiet react with explicit ownership for prompt/action policy.
     * When {@code speakerIsOwner} is false and actions are enabled, only stranger-safe tools run
     * with {@code notifySpeaker} as the approach target; speak lines go to owner and speaker.
     */
    public static boolean askQuiet(ServerPlayer owner, FabricCompanionEntity companion,
                                   String speakerName, String promptMessage,
                                   boolean speakerIsOwner, ServerPlayer notifySpeaker) {
        CompanionAiActionTrust trust = speakerIsOwner
                ? CompanionAiActionTrust.OWNER
                : CompanionAiActionTrust.STRANGER;
        return askQuiet(owner, companion, speakerName, promptMessage, trust, notifySpeaker);
    }

    public static boolean askQuiet(ServerPlayer owner, FabricCompanionEntity companion,
                                   String speakerName, String promptMessage,
                                   CompanionAiActionTrust trust, ServerPlayer notifySpeaker) {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        if (!runtime.isEnabled() || owner == null || companion == null || companion.isRemoved()) {
            return false;
        }
        if (!companion.isOwnedBy(owner)) {
            return false;
        }
        CompanionAiActionTrust effective = trust == null ? CompanionAiActionTrust.NONE : trust;
        boolean speakerIsOwner = effective.isOwner();
        if (!FtbCompat.mayAsk(owner)) {
            return false;
        }
        String censored = censorPrompt(promptMessage, runtime.settings(), speakerIsOwner);
        CompanionChatContext ctx = buildContext(companion,
                speakerName == null || speakerName.isBlank() ? owner.getGameProfile().getName() : speakerName,
                censored, runtime, speakerIsOwner);
        notifyThinking(owner, companion, true);
        if (notifySpeaker != null && notifySpeaker.isAlive()
                && !notifySpeaker.getUUID().equals(owner.getUUID())) {
            notifyThinking(notifySpeaker, companion, true);
        }
        boolean accepted = runtime.requestChatAsync(ctx, (reply, error) -> {
            MinecraftServer server = owner.getServer();
            if (server == null) {
                return;
            }
            server.execute(() -> {
                notifyThinking(owner, companion, false);
                if (notifySpeaker != null) {
                    notifyThinking(notifySpeaker, companion, false);
                }
                if (!owner.isAlive() || companion.isRemoved() || !companion.isOwnedBy(owner)) {
                    return;
                }
                if (error != null || reply == null || reply.isBlank()) {
                    return;
                }
                String clipped = reply.length() > 512 ? reply.substring(0, 509) + "…" : reply;
                CompanionAiSettings settings = CompanionAiRuntime.get().settings();
                boolean runActions = effective.allowsActions()
                        && companion.isAiModeEnabled()
                        && FtbCompat.mayAiActions(owner);
                CompanionAiActionParser.ParsedReply parsed = runActions
                        ? CompanionAiActionParser.parse(clipped)
                        : new CompanionAiActionParser.ParsedReply(clipped, java.util.List.of());
                String speak = parsed.speakText().isBlank() ? (parsed.hasActions() ? "" : clipped) : parsed.speakText();
                if (!speak.isBlank()) {
                    String line = speak.length() > 512 ? speak.substring(0, 509) + "…" : speak;
                    line = CompanionChatCensor.censorOutput(line, settings);
                    companion.speakLine(line);
                    notifySpeakerLine(companion, owner, notifySpeaker, line);
                }
                if (runActions && parsed.hasActions()) {
                    var filtered = effective.filter(parsed.actions());
                    if (!filtered.isEmpty()) {
                        FabricCompanionAiActionExecutor.execute(
                                companion, owner, filtered, settings, notifySpeaker, effective.fullControl());
                    }
                }
            });
        });
        if (!accepted) {
            notifyThinking(owner, companion, false);
            if (notifySpeaker != null) {
                notifyThinking(notifySpeaker, companion, false);
            }
        }
        return accepted;
    }

    private static void notifyThinking(ServerPlayer player, FabricCompanionEntity companion, boolean active) {
        if (player == null || !player.isAlive()) {
            return;
        }
        if (!active && CompanionAiRuntime.get().isBusy()) {
            return;
        }
        CompanionAiSettings settings = CompanionAiRuntime.get().settings();
        if (active) {
            String name = companion == null ? "Companion" : companion.getChatDisplayName();
            FabricNetworking.sendAiThinking(player, true, name, settings.timeoutSeconds(), -1f);
        } else {
            FabricNetworking.sendAiThinking(player, false, "", 0, -1f);
        }
    }

    private static String censorPrompt(String promptMessage, CompanionAiSettings settings, boolean speakerIsOwner) {
        String normalized = CompanionAiInput.normalize(promptMessage, settings);
        String censored = CompanionProfanityFilter.maybeCensor(settings.censorChat(), normalized);
        if (!speakerIsOwner) {
            return CompanionChatCensor.censorStrangerInput(censored, settings);
        }
        return CompanionChatCensor.censorOutput(censored, settings);
    }

    private static void notifySpeakerLine(FabricCompanionEntity companion, ServerPlayer owner,
                                          ServerPlayer notifySpeaker, String line) {
        if (notifySpeaker == null || !notifySpeaker.isAlive() || line == null || line.isBlank()) {
            return;
        }
        if (owner != null && notifySpeaker.getUUID().equals(owner.getUUID())) {
            return;
        }
        notifySpeaker.displayClientMessage(
                Component.literal("<" + companion.getChatDisplayName() + "> " + line), false);
    }

    private static void deliver(ServerPlayer player, FabricCompanionEntity companion, String reply, Throwable error,
                                boolean reportErrors) {
        if (!player.isAlive() || companion.isRemoved()) {
            return;
        }
        if (error != null) {
            if (reportErrors) {
                player.displayClientMessage(Component.literal("Companion AI error: " + error.getMessage()), false);
            }
            return;
        }
        if (reply == null || reply.isBlank()) {
            if (reportErrors) {
                player.displayClientMessage(Component.literal("Companion AI returned an empty reply."), false);
            }
            return;
        }
        String clipped = reply.length() > 512 ? reply.substring(0, 509) + "…" : reply;
        CompanionAiSettings settings = CompanionAiRuntime.get().settings();
        boolean allowActions = companion.isAiModeEnabled() && FtbCompat.mayAiActions(player);
        CompanionAiActionParser.ParsedReply parsed = allowActions
                ? CompanionAiActionParser.parse(clipped)
                : new CompanionAiActionParser.ParsedReply(clipped, java.util.List.of());
        String speak = parsed.speakText().isBlank() ? (parsed.hasActions() ? "" : clipped) : parsed.speakText();
        if (!speak.isBlank()) {
            companion.speakLine(speak.length() > 512 ? speak.substring(0, 509) + "…" : speak);
        }
        if (allowActions && parsed.hasActions() && companion.isOwnedBy(player)) {
            FabricCompanionAiActionExecutor.execute(companion, player, parsed.actions(), settings);
        }
    }

    /** Nearest companion owned by {@code player} within {@code range}. */
    public static FabricCompanionEntity findNearestOwned(ServerPlayer player, double range) {
        if (player == null) {
            return null;
        }
        AABB box = player.getBoundingBox().inflate(range);
        return player.level().getEntitiesOfClass(FabricCompanionEntity.class, box, c -> c.isOwnedBy(player))
                .stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(player)))
                .orElse(null);
    }

    /**
     * Owned companion whose display name matches {@code nameQuery} (sanitized), nearest preferred.
     * Only considers companions owned by {@code player} — never another player's name collision.
     */
    public static FabricCompanionEntity findOwnedByName(ServerPlayer player, String nameQuery, double range) {
        if (player == null || nameQuery == null || CompanionAskResolve.sanitizeToken(nameQuery).isEmpty()) {
            return null;
        }
        AABB box = player.getBoundingBox().inflate(range);
        return player.level().getEntitiesOfClass(FabricCompanionEntity.class, box,
                        c -> c.isOwnedBy(player) && CompanionAskResolve.namesMatch(c.getChatDisplayName(), nameQuery))
                .stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(player)))
                .orElse(null);
    }

    public static boolean ownsCompanionNamed(ServerPlayer player, String nameQuery, double range) {
        return findOwnedByName(player, nameQuery, range) != null;
    }

    /**
     * Name-address target: prefer the speaker's owned companion; else nearest matching name
     * whose owner is online (stranger address). World actions must stay owner-gated by caller.
     */
    public static FabricCompanionEntity findAddressedCompanion(ServerPlayer speaker, String nameQuery, double range) {
        FabricCompanionEntity owned = findOwnedByName(speaker, nameQuery, range);
        if (owned != null) {
            return owned;
        }
        if (speaker == null || nameQuery == null || CompanionAskResolve.sanitizeToken(nameQuery).isEmpty()) {
            return null;
        }
        AABB box = speaker.getBoundingBox().inflate(range);
        MinecraftServer server = speaker.getServer();
        return speaker.level().getEntitiesOfClass(FabricCompanionEntity.class, box, c -> {
                    if (!CompanionAskResolve.namesMatch(c.getChatDisplayName(), nameQuery)) {
                        return false;
                    }
                    UUID ownerId = c.getOwnerUuid();
                    return ownerId != null && server != null && server.getPlayerList().getPlayer(ownerId) != null;
                })
                .stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(speaker)))
                .orElse(null);
    }

    /**
     * Companion whose display name is mentioned in chat ({@link CompanionNameMention}).
     * Prefers the speaker's owned companion, else nearest with an online owner.
     */
    public static FabricCompanionEntity findMentionedCompanion(ServerPlayer speaker, String rawMessage, double range) {
        if (speaker == null || rawMessage == null || rawMessage.isBlank()) {
            return null;
        }
        AABB box = speaker.getBoundingBox().inflate(range);
        MinecraftServer server = speaker.getServer();
        return speaker.level().getEntitiesOfClass(FabricCompanionEntity.class, box, c -> {
                    if (!c.isAlive()) {
                        return false;
                    }
                    UUID ownerId = c.getOwnerUuid();
                    if (ownerId == null || server == null || server.getPlayerList().getPlayer(ownerId) == null) {
                        return false;
                    }
                    return CompanionNameMention.messageMentionsName(rawMessage, c.getChatDisplayName());
                })
                .stream()
                .min(Comparator
                        .<FabricCompanionEntity>comparingInt(c -> c.isOwnedBy(speaker) ? 0 : 1)
                        .thenComparingDouble(c -> c.distanceToSqr(speaker)))
                .orElse(null);
    }

    public static FabricCompanionEntity findReactCompanion(ServerPlayer speaker, ChatListenMode mode, double range) {
        if (speaker == null || mode == null || !mode.listens()) {
            return null;
        }
        AABB box = speaker.getBoundingBox().inflate(range);
        if (mode == ChatListenMode.PLAYER) {
            return speaker.level().getEntitiesOfClass(FabricCompanionEntity.class, box, c -> c.isOwnedBy(speaker))
                    .stream()
                    .min(Comparator.comparingDouble(c -> c.distanceToSqr(speaker)))
                    .orElse(null);
        }
        // global: nearest owned companion to the speaker among any online owner's companions in range
        List<FabricCompanionEntity> near = speaker.level().getEntitiesOfClass(
                FabricCompanionEntity.class, box, c -> {
                    UUID ownerId = c.getOwnerUuid();
                    if (ownerId == null) {
                        return false;
                    }
                    MinecraftServer server = speaker.getServer();
                    return server != null && server.getPlayerList().getPlayer(ownerId) != null;
                });
        return near.stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(speaker)))
                .orElse(null);
    }

    public static String status() {
        return CompanionAiRuntime.get().statusLine();
    }

    private static CompanionChatContext buildContext(FabricCompanionEntity companion, String speaker,
                                                     String message, CompanionAiRuntime runtime) {
        return buildContext(companion, speaker, message, runtime, true);
    }

    private static CompanionChatContext buildContext(FabricCompanionEntity companion, String speaker,
                                                     String message, CompanionAiRuntime runtime,
                                                     boolean speakerIsOwner) {
        boolean child = companion.getLeaderUuid() != null;
        String parentName = "";
        if (child && companion.level() instanceof net.minecraft.server.level.ServerLevel level) {
            var parent = level.getEntity(companion.getLeaderUuid());
            if (parent instanceof FabricCompanionEntity pe) {
                parentName = pe.getChatDisplayName();
            }
        }
        return new CompanionChatContext(
                companion.getUUID(),
                companion.getChatDisplayName(),
                companion.getForm().serializedName(),
                companion.getAttitude().serializedName(),
                speaker,
                message,
                runtime.settings().inputLanguage(),
                parentName,
                child,
                speakerIsOwner,
                List.of(),
                companion.getPersona(),
                companion.isAiModeEnabled()
        );
    }
}
