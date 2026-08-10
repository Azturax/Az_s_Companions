package com.azscompanions.event;

import com.azscompanions.ai.ChatListenMode;
import com.azscompanions.ai.CompanionAiActionTrust;
import com.azscompanions.ai.CompanionAiChatSupport;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.ai.CompanionAskResolve;
import com.azscompanions.ai.CompanionNameMention;
import com.azscompanions.ai.FabricCompanionAiAsk;
import com.azscompanions.command.FabricCompanionCommands;
import com.azscompanions.compat.ftb.FtbCompat;
import com.azscompanions.entity.FabricCompanionEntity;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Fabric: named ask, name-mention (owner vs stranger), and chatListenMode reactions.
 * Stranger name mentions do not cancel public chat broadcast.
 */
public final class FabricCompanionAiChatEvents {
    private FabricCompanionAiChatEvents() {
    }

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            String raw = message.decoratedContent().getString();
            return !tryNamedAsk(sender, raw);
        });
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            onPlayerChat(sender, message.decoratedContent().getString());
        });
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
        FabricCompanionEntity companion = FabricCompanionAiAsk.findOwnedByName(
                speaker, parsed.get().companionName(), FabricCompanionCommands.ASK_RANGE);
        if (companion == null) {
            return false;
        }
        return FabricCompanionAiAsk.ask(speaker, companion, parsed.get().message()) > 0;
    }

    static void onPlayerChat(ServerPlayer speaker, String rawText) {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        if (!runtime.isEnabled() || runtime.isBusy()) {
            return;
        }
        if (CompanionAiChatSupport.looksLikeCompanionReply(rawText)) {
            return;
        }
        CompanionAiSettings settings = runtime.settings();
        // Name mention works even when chatListenMode=off (before listen-mode gate).
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
        FabricCompanionEntity companion = FabricCompanionAiAsk.findReactCompanion(
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
        String prompt = ownerLike
                ? CompanionAiChatSupport.chatReactionPrompt(speaker.getGameProfile().getName(), rawText.trim())
                : CompanionAiChatSupport.strangerAddressPrompt(speaker.getGameProfile().getName(), rawText.trim());
        if (FabricCompanionAiAsk.askQuiet(owner, companion, speaker.getGameProfile().getName(), prompt,
                trust, ownerLike ? null : speaker)) {
            runtime.markChatReact(companion.getUUID(), ownerId);
        }
    }

    /**
     * {@code Bit, come here} — when {@code nameListen} (default true), independent of chatListenMode.
     * Does not cancel public chat.
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
        FabricCompanionEntity companion = FabricCompanionAiAsk.findMentionedCompanion(
                speaker, rawText, settings.chatReactRange());
        if (companion == null) {
            return false;
        }
        UUID ownerId = companion.getOwnerUuid();
        if (!runtime.canChatReact(companion.getUUID(), ownerId)) {
            return false;
        }
        ServerPlayer owner = companion.getOwner() instanceof ServerPlayer sp ? sp : null;
        if (owner == null) {
            return false;
        }
        boolean speakerIsOwner = companion.isOwnedBy(speaker);
        CompanionAiActionTrust trust = FtbCompat.resolveTrust(
                speakerIsOwner, companion.getOwnerUuid(), speaker.getUUID());
        boolean ownerLike = trust.isOwner();
        String prompt = CompanionNameMention.mentionPrompt(
                speaker.getGameProfile().getName(), rawText.trim(), ownerLike);
        boolean ok = FabricCompanionAiAsk.askQuiet(owner, companion, speaker.getGameProfile().getName(), prompt,
                trust, ownerLike ? null : speaker);
        if (ok) {
            runtime.markChatReact(companion.getUUID(), ownerId);
        }
        return ok;
    }
}
