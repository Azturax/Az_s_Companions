package com.azscompanions.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

/**
 * Chat auto-react / name-mention retired in 0.3.12 — companions reply only via {@code /ask} / {@code /az ask}.
 */
public final class CompanionAiChatEvents {
    private CompanionAiChatEvents() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        // no-op: ask-only
    }
}
