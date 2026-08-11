package com.azscompanions.ai;

/**
 * One short-lived observation near the owner for ambient / reactive chat.
 *
 * @param kind       event category
 * @param gameTime   server game time when recorded
 * @param detail     human-readable context for LLM prompts (item name, etc.)
 * @param itemId     optional registry id ({@code minecraft:diamond_sword})
 * @param reactive   when true, may trigger an early chatter tick (then consumed)
 */
public record CompanionRecentAction(
        CompanionRecentActionKind kind,
        long gameTime,
        String detail,
        String itemId,
        boolean reactive
) {
    public CompanionRecentAction {
        if (kind == null) {
            throw new IllegalArgumentException("kind");
        }
        detail = detail == null ? "" : detail.trim();
        itemId = itemId == null || itemId.isBlank() ? null : itemId.trim().toLowerCase();
    }

    public CompanionRecentAction withReactive(boolean value) {
        return new CompanionRecentAction(kind, gameTime, detail, itemId, value);
    }
}
