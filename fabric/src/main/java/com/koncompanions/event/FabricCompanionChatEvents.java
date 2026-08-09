package com.koncompanions.event;

import com.koncompanions.entity.FabricCompanionEntity;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;

/** Fabric parity: owner chat near Kon → canned replies. */
public final class FabricCompanionChatEvents {
    private static final double REPLY_RANGE = 16.0d;

    private FabricCompanionChatEvents() {
    }

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            ServerPlayer player = sender;
            String raw = message.signedContent();
            if (raw == null || raw.isBlank()) {
                return;
            }
            FabricCompanionEntity companion = player.level().getEntitiesOfClass(
                            FabricCompanionEntity.class,
                            player.getBoundingBox().inflate(REPLY_RANGE),
                            c -> c.isOwnedBy(player))
                    .stream()
                    .min(Comparator.comparingDouble(c -> c.distanceToSqr(player)))
                    .orElse(null);
            if (companion != null) {
                companion.tryReplyToChat(raw);
            }
        });
    }
}
