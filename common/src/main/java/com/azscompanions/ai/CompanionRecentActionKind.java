package com.azscompanions.ai;

/**
 * Short-lived world/player events companions can chatter about.
 * Priority is used when picking one reactive trigger (higher = sooner).
 */
public enum CompanionRecentActionKind {
    EXPLOSION(100),
    DARKNESS(90),
    ITEM_CRAFT(85),
    CRAFT_READY(80),
    ITEM_FIND(70),
    /** Host-defined {@link CompanionCustomChatEvent} reaction. */
    CUSTOM(65),
    DAMAGE(60),
    COMBAT(55),
    EATING(40),
    SLEEPING(35),
    BLOCK_PLACE(20),
    BLOCK_BREAK(15);

    private final int priority;

    CompanionRecentActionKind(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
