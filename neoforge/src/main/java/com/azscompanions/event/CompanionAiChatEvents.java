package com.azscompanions.event;

import com.azscompanions.ai.ChatListenMode;
import com.azscompanions.ai.CompanionAiAsk;
import com.azscompanions.ai.CompanionAiChatSupport;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionChatListenSupport;
import com.azscompanions.ai.CompanionNameMention;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.UUID;

/**
 * Public chat → one spawned companion (name mention preferred, else nearby listen).
 */
public final class CompanionAiChatEvents {
    private CompanionAiChatEvents() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer speaker = event.getPlayer();
        String raw = event.getRawText();
        if (speaker == null || CompanionChatListenSupport.shouldIgnoreChat(raw)) {
            return;
        }
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        if (!runtime.isEnabled()) {
            return;
        }
        double range = runtime.settings().chatReactRange();
        CompanionEntity mentioned = CompanionAiAsk.findMentionedCompanion(speaker, raw, range);
        CompanionEntity target;
        boolean addressed;
        if (mentioned != null && mentioned.getChatListenMode().listens()) {
            target = mentioned;
            addressed = true;
        } else {
            target = pickNearby(speaker, range);
            addressed = false;
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        UUID ownerId = target.getOwnerUuid();
        if (ownerId == null || !runtime.canChatReact(target.getUUID(), ownerId)) {
            return;
        }
        MinecraftServer server = speaker.getServer();
        if (server == null) {
            return;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner == null || !owner.isAlive()) {
            return;
        }
        boolean speakerIsOwner = target.isOwnedBy(speaker);
        ChatListenMode mode = target.getChatListenMode();
        if (!addressed) {
            if (mode == ChatListenMode.PLAYER && !speakerIsOwner) {
                return;
            }
            if (mode == ChatListenMode.GLOBAL && !speakerIsOwner
                    && !CompanionChatListenSupport.allowNearbyReply(mode)) {
                return;
            }
        }
        runtime.markChatReact(target.getUUID(), ownerId);
        String speakerName = speaker.getGameProfile().getName();
        String prompt = addressed
                ? CompanionNameMention.mentionPrompt(speakerName, raw, speakerIsOwner)
                : CompanionAiChatSupport.chatReactionPrompt(speakerName, raw);
        CompanionAiAsk.askQuiet(owner, target, speakerName, prompt, speakerIsOwner, speaker);
    }

    private static CompanionEntity pickNearby(ServerPlayer speaker, double range) {
        CompanionEntity owned = CompanionAiAsk.findNearestOwned(speaker, range);
        if (owned != null && owned.getChatListenMode().listens()) {
            return owned;
        }
        CompanionEntity any = CompanionAiAsk.findReactCompanion(speaker, ChatListenMode.GLOBAL, range);
        if (any != null && any.getChatListenMode() == ChatListenMode.GLOBAL) {
            return any;
        }
        return null;
    }
}
