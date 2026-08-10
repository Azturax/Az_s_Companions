package com.azscompanions.event;

/**
 * Chat auto-react / name-mention retired in 0.3.12 — companions reply only via {@code /ask} / {@code /az ask}.
 */
public final class FabricCompanionAiChatEvents {
    private FabricCompanionAiChatEvents() {
    }

    public static void register() {
        // no-op: ask-only (keep method so bootstrap still compiles)
    }
}
