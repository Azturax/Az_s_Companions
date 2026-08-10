package com.azscompanions.event;

import com.azscompanions.ai.ChatListenMode;
import com.azscompanions.ai.CompanionAiAsk;
import com.azscompanions.ai.CompanionAiActionTrust;
import com.azscompanions.ai.CompanionAiChatSupport;
import com.azscompanions.ai.CompanionAiInput;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.ai.CompanionAskResolve;
import com.azscompanions.ai.CompanionNameMention;
import com.azscompanions.command.CompanionCommands;
import com.azscompanions.compat.ftb.FtbCompat;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.UUID;

/**
 * NeoForge: pure-chat name-mention (primary), optional {@code Name ask …}, and chatListenMode.
 * Talk in chat using their name — slash {@code /ask} is not required.
 * Stranger name mentions do not cancel public chat broadcast.
 */
public final class CompanionAiChatEvents {
    private CompanionAiChatEvents() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer speaker = event.getPlayer();
        String rawText = event.getRawText();
        if (tryNamedAsk(speaker, rawText)) {
            event.setCanceled(true);
            return;
        }

        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        if (!runtime.isEnabled()) {
            return;
        }
        if (rawText != null && rawText.trim().startsWith("/")) {
            return;
        }
        if (CompanionAiChatSupport.looksLikeCompanionReply(rawText)) {
            return;
        }

        CompanionAiSettings settings = runtime.settings();
        // Name mention is the primary path — works with chatListenMode=off; queues when busy.
        if (tryNameMention(speaker, rawText, settings)) {
            return;
        }

        ChatListenMode mode = settings.chatListenMode();
        if (!mode.listens()) {
            return;
        }
        if (CompanionAiChatSupport.shouldIgnoreChatMessage(rawText)) {
            return;
        }
        CompanionEntity companion = CompanionAiAsk.findReactCompanion(
                speaker, mode, settings.chatReactRange());
        if (companion == null) {
            return;
        }
        UUID ownerId = companion.getOwnerUuid();
        if (!runtime.canChatReact(companion.getUUID(), ownerId)) {
            return;
        }
        ServerPlayer owner = companion.getOwner() instanceof ServerPlayer sp ? sp : null;
        if (owner == null) {
            return;
        }
        boolean speakerIsOwner = companion.isOwnedBy(speaker);
        CompanionAiActionTrust trust = FtbCompat.resolveTrust(
                speakerIsOwner, companion.getOwnerUuid(), speaker.getUUID());
        boolean ownerLike = trust.isOwner();
        String full = CompanionAiInput.normalize(rawText, settings);
        String prompt = ownerLike
                ? CompanionAiChatSupport.chatReactionPrompt(speaker.getGameProfile().getName(), full)
                : CompanionAiChatSupport.strangerAddressPrompt(speaker.getGameProfile().getName(), full);
        if (CompanionAiAsk.askQuiet(owner, companion, speaker.getGameProfile().getName(), prompt,
                trust, ownerLike ? null : speaker)) {
            runtime.markChatReact(companion.getUUID(), ownerId);
        }
    }

    static boolean tryNamedAsk(ServerPlayer speaker, String rawText) {
        var parsed = CompanionAskResolve.parseNamedAskChat(rawText);
        if (parsed.isEmpty()) {
            return false;
        }
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        if (!runtime.isEnabled()) {
            return false;
        }
        CompanionEntity companion = CompanionAiAsk.findOwnedByName(
                speaker, parsed.get().companionName(), CompanionCommands.ASK_RANGE);
        if (companion == null) {
            return false;
        }
        return CompanionAiAsk.ask(speaker, companion, parsed.get().message()) > 0;
    }

    /**
     * Pure chat: {@code Bit, come here} / {@code Kon how are you?} when {@code nameListen}
     * (default true). Independent of chatListenMode. Does not cancel public chat.
     * Owner name-mentions skip the auto-react cooldown so rapid multi-sentence talk works.
     */
    static boolean tryNameMention(ServerPlayer speaker, String rawText, CompanionAiSettings settings) {
        if (!settings.nameListen() || rawText == null || rawText.startsWith("/")) {
            return false;
        }
        if (CompanionAskResolve.parseNamedAskChat(rawText).isPresent()) {
            return false;
        }
        if (CompanionAiChatSupport.looksLikeCompanionReply(rawText)) {
            return false;
        }
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        CompanionEntity companion = CompanionAiAsk.findMentionedCompanion(
                speaker, rawText, settings.chatReactRange());
        if (companion == null) {
            return false;
        }
        UUID ownerId = companion.getOwnerUuid();
        boolean speakerIsOwner = companion.isOwnedBy(speaker);
        // Strangers still use cooldown (spam guard); owners always get through (queue if busy).
        if (!speakerIsOwner && !runtime.canChatReact(companion.getUUID(), ownerId)) {
            return false;
        }
        ServerPlayer owner = companion.getOwner() instanceof ServerPlayer sp ? sp : null;
        if (owner == null) {
            return false;
        }
        CompanionAiActionTrust trust = FtbCompat.resolveTrust(
                speakerIsOwner, companion.getOwnerUuid(), speaker.getUUID());
        boolean ownerLike = trust.isOwner();
        String full = CompanionAiInput.normalize(rawText, settings);
        String prompt = CompanionNameMention.mentionPrompt(
                speaker.getGameProfile().getName(), full, ownerLike);
        boolean ok = CompanionAiAsk.askQuiet(owner, companion, speaker.getGameProfile().getName(), prompt,
                trust, ownerLike ? null : speaker);
        if (ok && !speakerIsOwner) {
            runtime.markChatReact(companion.getUUID(), ownerId);
        }
        return ok;
    }
}
