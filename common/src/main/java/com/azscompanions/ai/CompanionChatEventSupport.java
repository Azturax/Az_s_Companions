package com.azscompanions.ai;

import java.util.List;
import java.util.UUID;

/** Gates builtin reactive chatter and fans out host-defined CompanionCustomChatEvent entries. */
public final class CompanionChatEventSupport {
    private CompanionChatEventSupport() {
    }

    public static CompanionAiSettings settings() {
        return CompanionAiRuntime.get().settings();
    }

    public static boolean allowBuiltinReactive(CompanionRecentActionKind kind) {
        return allowBuiltinReactive(settings(), kind);
    }

    public static boolean allowBuiltinReactive(CompanionAiSettings s, CompanionRecentActionKind kind) {
        if (s == null || kind == null || kind == CompanionRecentActionKind.CUSTOM) {
            return false;
        }
        if (!s.reactiveChat()) {
            return false;
        }
        if (kind == CompanionRecentActionKind.ITEM_FIND && !s.itemFindChat()) {
            return false;
        }
        return true;
    }

    public static boolean allowReactiveAction(CompanionAiSettings s, CompanionRecentAction action) {
        if (s == null || action == null || !s.reactiveChat()) {
            return false;
        }
        if (action.kind() == CompanionRecentActionKind.ITEM_FIND && !s.itemFindChat()) {
            return false;
        }
        if (action.kind() == CompanionRecentActionKind.CUSTOM) {
            CompanionCustomChatEvent ev = findById(s, action.customEventId());
            return ev != null && ev.enabled();
        }
        return true;
    }

    public static CompanionCustomChatEvent findById(CompanionAiSettings s, String id) {
        if (s == null || id == null || id.isBlank()) {
            return null;
        }
        String key = id.trim().toLowerCase(java.util.Locale.ROOT);
        for (CompanionCustomChatEvent e : s.customChatEvents()) {
            if (key.equals(e.id())) {
                return e;
            }
        }
        return null;
    }

    public static boolean observe(
            UUID playerId, long gameTime, CompanionRecentActionKind kind,
            String detail, String itemId, boolean reactive) {
        if (kind == CompanionRecentActionKind.ITEM_FIND) {
            if (!CompanionRecentActionMemory.tryClaimItemFind(playerId, gameTime)) {
                return false;
            }
            if (allowBuiltinReactive(kind)) {
                CompanionRecentActionMemory.record(
                        playerId, gameTime, kind, detail, itemId, reactive, true);
            }
            fanOutCustom(playerId, gameTime, kind, itemId);
            return true;
        }

        boolean recorded = false;
        if (allowBuiltinReactive(kind)) {
            recorded = CompanionRecentActionMemory.record(
                    playerId, gameTime, kind, detail, itemId, reactive);
        }
        fanOutCustom(playerId, gameTime, kind, itemId);
        return recorded;
    }

    public static boolean observeDarknessEnter(UUID playerId, long gameTime, boolean currentlyDark) {
        boolean allow = allowBuiltinReactive(CompanionRecentActionKind.DARKNESS);
        boolean entered = CompanionRecentActionMemory.recordDarknessEnter(
                playerId, gameTime, currentlyDark, allow);
        if (entered) {
            fanOutCustom(playerId, gameTime, CompanionRecentActionKind.DARKNESS, null);
        }
        return entered && allow;
    }

    public static void fanOutCustom(UUID playerId, long gameTime, CompanionRecentActionKind kind, String itemId) {
        CompanionAiSettings s = settings();
        if (playerId == null || kind == null || s == null || !s.reactiveChat()) {
            return;
        }
        for (CompanionCustomChatEvent e : s.customChatEvents()) {
            if (!e.isValid() || !e.enabled() || !e.matchesTrigger(kind) || !e.matchesItem(itemId)) {
                continue;
            }
            CompanionRecentActionMemory.recordCustom(playerId, gameTime, e, itemId);
        }
    }

    public static CompanionCustomChatEvent pickIdleCustom(CompanionAiSettings s, UUID playerId, long gameTime) {
        if (s == null || !s.idleChat() || playerId == null) {
            return null;
        }
        List<CompanionCustomChatEvent> idle = s.customChatEvents().stream()
                .filter(CompanionCustomChatEvent::isValid)
                .filter(CompanionCustomChatEvent::enabled)
                .filter(e -> "idle".equals(e.trigger()))
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .toList();
        for (CompanionCustomChatEvent e : idle) {
            if (CompanionRecentActionMemory.canRecordCustom(playerId, gameTime, e)) {
                return e;
            }
        }
        return null;
    }
}