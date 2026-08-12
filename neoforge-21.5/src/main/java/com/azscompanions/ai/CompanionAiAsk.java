package com.azscompanions.ai;

import com.azscompanions.util.OwnableUuids;

import com.azscompanions.compat.ftb.FtbCompat;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.network.packet.CompanionAiThinkingPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * NeoForge helper: ask nearby owned companion via configured LLM / MCP provider.
 * Replies are text dialogue for every {@link com.azscompanions.entity.CompanionForm}.
 * Requires server AI config (provider ≠ disabled). Text dialogue only — no AI Mode world control.
 */
public final class CompanionAiAsk {
    private CompanionAiAsk() {
    }

    public static int ask(ServerPlayer player, CompanionEntity companion, String message) {
        return ask(player, companion, message, true, true);
    }

    public static int ask(ServerPlayer player, CompanionEntity companion, String message,
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
                        "Companion AI isn't configured here. Set a provider in /az admin → AI Config (or config/azscompanions-ai.toml) — local LM Studio/Ollama or a remote API. On dedicated servers, AI runs on the host only."), false);
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
        boolean showChat = runtime.settings().enableChatMessages();
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
                deliver(player, companion, reply, error, showChat, reportErrors);
            });
        });
        if (!accepted) {
            notifyThinking(player, companion, false);
        }
        return accepted ? 1 : 0;
    }

    public static boolean askQuiet(ServerPlayer owner, CompanionEntity companion,
                                   String speakerName, String promptMessage) {
        return askQuiet(owner, companion, speakerName, promptMessage, true, null);
    }

    public static boolean askQuiet(ServerPlayer owner, CompanionEntity companion,
                                   String speakerName, String promptMessage, boolean allowActions) {
        return askQuiet(owner, companion, speakerName, promptMessage,
                allowActions ? CompanionAiActionTrust.OWNER : CompanionAiActionTrust.NONE, null);
    }

    /**
     * Quiet react with explicit ownership for prompt/action policy.
     * When {@code speakerIsOwner} is false and actions are enabled, only stranger-safe tools run
     * with {@code notifySpeaker} as the approach target; speak lines go to owner and speaker.
     */
    public static boolean askQuiet(ServerPlayer owner, CompanionEntity companion,
                                   String speakerName, String promptMessage,
                                   boolean speakerIsOwner, ServerPlayer notifySpeaker) {
        CompanionAiActionTrust trust = speakerIsOwner
                ? CompanionAiActionTrust.OWNER
                : CompanionAiActionTrust.STRANGER;
        return askQuiet(owner, companion, speakerName, promptMessage, trust, notifySpeaker, null);
    }

    /**
     * @param trust {@link CompanionAiActionTrust#OWNER} full tools, {@link CompanionAiActionTrust#STRANGER}
     *              social allowlist, {@link CompanionAiActionTrust#NONE} dialogue only
     * @param notifySpeaker non-owner speaker who should also see speak lines / be approached
     */
    public static boolean askQuiet(ServerPlayer owner, CompanionEntity companion,
                                   String speakerName, String promptMessage,
                                   CompanionAiActionTrust trust, ServerPlayer notifySpeaker) {
        return askQuiet(owner, companion, speakerName, promptMessage, trust, notifySpeaker, null);
    }

    /**
     * @param fallbackLine spoken if the LLM errors or returns blank (ambient / call-away); null = silent fail
     */
    public static boolean askQuiet(ServerPlayer owner, CompanionEntity companion,
                                   String speakerName, String promptMessage,
                                   CompanionAiActionTrust trust, ServerPlayer notifySpeaker,
                                   String fallbackLine) {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        if (!runtime.isEnabled() || owner == null || companion == null || companion.isRemoved()
                || !companion.isOwnedBy(owner)) {
            return false;
        }
        CompanionAiActionTrust effective = trust == null ? CompanionAiActionTrust.NONE : trust;
        boolean showChat = runtime.settings().enableChatMessages();
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
                && (owner == null || !notifySpeaker.getUUID().equals(owner.getUUID()))) {
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
                    if (fallbackLine != null && !fallbackLine.isBlank()) {
                        companion.speakLine(fallbackLine);
                    }
                    return;
                }
                String clipped = reply.length() > 512 ? reply.substring(0, 509) + "…" : reply;
                CompanionAiSettings settings = CompanionAiRuntime.get().settings();
                String speak = CompanionAiActionParser.parse(clipped).speakText();
                if (speak.isBlank()) {
                    speak = clipped;
                }
                if (!speak.isBlank()) {
                    String line = speak.length() > 512 ? speak.substring(0, 509) + "…" : speak;
                    line = CompanionChatCensor.censorOutput(line, settings);
                    if (showChat) {
                        companion.speakLine(line);
                    } else {
                        owner.displayClientMessage(Component.literal(line), false);
                    }
                    notifySpeakerLine(companion, owner, notifySpeaker, line);
                } else if (fallbackLine != null && !fallbackLine.isBlank()) {
                    companion.speakLine(fallbackLine);
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

    private static void notifyThinking(ServerPlayer player, CompanionEntity companion, boolean active) {
        if (player == null || !player.isAlive()) {
            return;
        }
        // Keep HUD up while another queued request is still in flight.
        if (!active && CompanionAiRuntime.get().isBusy()) {
            return;
        }
        CompanionAiSettings settings = CompanionAiRuntime.get().settings();
        if (active) {
            String name = companion == null ? "Companion" : companion.getChatDisplayName();
            PacketDistributor.sendToPlayer(player,
                    CompanionAiThinkingPacket.start(name, settings.timeoutSeconds()));
        } else {
            PacketDistributor.sendToPlayer(player, CompanionAiThinkingPacket.stop());
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

    private static void notifySpeakerLine(CompanionEntity companion, ServerPlayer owner,
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

    private static void deliver(ServerPlayer player, CompanionEntity companion, String reply, Throwable error,
                                boolean showChat, boolean reportErrors) {
        if (!player.isAlive() || companion.isRemoved()) {
            return;
        }
        if (error != null) {
            if (reportErrors) {
                player.displayClientMessage(Component.literal(
                        CompanionAiChatSupport.playerFacingAiError(error)), false);
            }
            return;
        }
        if (reply == null || reply.isBlank()) {
            if (reportErrors) {
                player.displayClientMessage(Component.literal(
                        "Companion AI returned an empty reply. "
                                + "Check model id; for Gemma 4 disable thinking / raise maxTokens. See server log."), false);
            }
            return;
        }
        String clipped = reply.length() > 512 ? reply.substring(0, 509) + "…" : reply;
        String speak = CompanionAiActionParser.parse(clipped).speakText();
        if (speak.isBlank()) {
            speak = clipped;
        }
        if (!speak.isBlank()) {
            if (showChat) {
                companion.speakLine(speak.length() > 512 ? speak.substring(0, 509) + "…" : speak);
            } else {
                player.displayClientMessage(Component.literal(speak), false);
            }
        }
    }

    public static CompanionEntity findNearestOwned(ServerPlayer player, double range) {
        if (player == null) {
            return null;
        }
        AABB box = player.getBoundingBox().inflate(range);
        return player.level().getEntitiesOfClass(CompanionEntity.class, box, c -> c.isOwnedBy(player))
                .stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(player)))
                .orElse(null);
    }

    public static CompanionEntity findOwnedByName(ServerPlayer player, String nameQuery, double range) {
        if (player == null || nameQuery == null || CompanionAskResolve.sanitizeToken(nameQuery).isEmpty()) {
            return null;
        }
        AABB box = player.getBoundingBox().inflate(range);
        return player.level().getEntitiesOfClass(CompanionEntity.class, box,
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
     * whose owner is online (stranger address). Never returns another player's companion for
     * ownership transfer — caller must gate world actions with {@code isOwnedBy(speaker)}.
     */
    public static CompanionEntity findAddressedCompanion(ServerPlayer speaker, String nameQuery, double range) {
        CompanionEntity owned = findOwnedByName(speaker, nameQuery, range);
        if (owned != null) {
            return owned;
        }
        if (speaker == null || nameQuery == null || CompanionAskResolve.sanitizeToken(nameQuery).isEmpty()) {
            return null;
        }
        AABB box = speaker.getBoundingBox().inflate(range);
        MinecraftServer server = speaker.getServer();
        return speaker.level().getEntitiesOfClass(CompanionEntity.class, box, c -> {
                    if (!CompanionAskResolve.namesMatch(c.getChatDisplayName(), nameQuery)) {
                        return false;
                    }
                    UUID ownerId = OwnableUuids.get(c);
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
    public static CompanionEntity findMentionedCompanion(ServerPlayer speaker, String rawMessage, double range) {
        if (speaker == null || rawMessage == null || rawMessage.isBlank()) {
            return null;
        }
        AABB box = speaker.getBoundingBox().inflate(range);
        MinecraftServer server = speaker.getServer();
        return speaker.level().getEntitiesOfClass(CompanionEntity.class, box, c -> {
                    if (!c.isAlive()) {
                        return false;
                    }
                    UUID ownerId = OwnableUuids.get(c);
                    if (ownerId == null || server == null || server.getPlayerList().getPlayer(ownerId) == null) {
                        return false;
                    }
                    return CompanionNameMention.messageMentionsName(rawMessage, c.getChatDisplayName());
                })
                .stream()
                .min(Comparator
                        .<CompanionEntity>comparingInt(c -> c.isOwnedBy(speaker) ? 0 : 1)
                        .thenComparingDouble(c -> c.distanceToSqr(speaker)))
                .orElse(null);
    }

    public static CompanionEntity findReactCompanion(ServerPlayer speaker, ChatListenMode mode, double range) {
        if (speaker == null || mode == null || !mode.listens()) {
            return null;
        }
        AABB box = speaker.getBoundingBox().inflate(range);
        if (mode == ChatListenMode.PLAYER) {
            return speaker.level().getEntitiesOfClass(CompanionEntity.class, box, c -> c.isOwnedBy(speaker))
                    .stream()
                    .min(Comparator.comparingDouble(c -> c.distanceToSqr(speaker)))
                    .orElse(null);
        }
        List<CompanionEntity> near = speaker.level().getEntitiesOfClass(
                CompanionEntity.class, box, c -> {
                    UUID ownerId = OwnableUuids.get(c);
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

    private static CompanionChatContext buildContext(CompanionEntity companion, String speaker,
                                                     String message, CompanionAiRuntime runtime) {
        return buildContext(companion, speaker, message, runtime, true);
    }

    private static CompanionChatContext buildContext(CompanionEntity companion, String speaker,
                                                     String message, CompanionAiRuntime runtime,
                                                     boolean speakerIsOwner) {
        boolean child = companion.getLeaderUuid() != null;
        String parentName = "";
        if (child && companion.level() instanceof net.minecraft.server.level.ServerLevel level) {
            var parent = level.getEntity(companion.getLeaderUuid());
            if (parent instanceof CompanionEntity pe) {
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
                companion.getPersona()
        );
    }
}
