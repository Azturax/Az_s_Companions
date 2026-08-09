package com.koncompanions.event;

import com.koncompanions.entity.CompanionEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.Comparator;

/**
 * Owner chat near Kon → canned wholesome replies (keyword match or name-addressed).
 */
public final class CompanionChatEvents {
    private static final double REPLY_RANGE = 16.0d;

    private CompanionChatEvents() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String raw = event.getRawText();
        if (raw == null || raw.isBlank()) {
            return;
        }

        CompanionEntity companion = player.level().getEntitiesOfClass(
                        CompanionEntity.class,
                        player.getBoundingBox().inflate(REPLY_RANGE),
                        c -> c.isOwnedBy(player))
                .stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(player)))
                .orElse(null);
        if (companion != null) {
            companion.tryReplyToChat(raw);
        }
    }
}
