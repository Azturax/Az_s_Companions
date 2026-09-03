package com.azscompanions.entity;

import com.azscompanions.ai.ChatListenMode;

/**
 * Per-companion NBT keys for player-facing AI / general settings
 * (persisted via {@link CompanionPlayerPersistence}).
 */
public final class CompanionPlayerAiPrefs {
    public static final String NBT_GLOBAL_TALK = "GlobalTalk";
    public static final String NBT_CHAT_LISTEN = "ChatListen";
    public static final String NBT_IDLE_CHAT = "IdleChat";
    public static final String NBT_TELEPORT = "TeleportEnabled";

    private CompanionPlayerAiPrefs() {
    }

    public static boolean defaultGlobalTalk() {
        return true;
    }

    public static boolean defaultIdleChat() {
        return true;
    }

    public static boolean defaultTeleport() {
        return true;
    }

    public static ChatListenMode defaultChatListen() {
        return ChatListenMode.GLOBAL;
    }

    public static ChatListenMode parseChatListen(String raw, ChatListenMode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback == null ? defaultChatListen() : fallback;
        }
        return ChatListenMode.fromConfig(raw);
    }

    public static boolean replyToChatEnabled(ChatListenMode mode) {
        return mode != null && mode.listens();
    }

    public static ChatListenMode cycleChatListen(ChatListenMode current) {
        if (current == ChatListenMode.OFF) {
            return ChatListenMode.PLAYER;
        }
        if (current == ChatListenMode.PLAYER) {
            return ChatListenMode.GLOBAL;
        }
        return ChatListenMode.OFF;
    }

    public static ChatListenMode fromReplyToggle(boolean replyToChat) {
        return replyToChat ? ChatListenMode.GLOBAL : ChatListenMode.OFF;
    }
}
