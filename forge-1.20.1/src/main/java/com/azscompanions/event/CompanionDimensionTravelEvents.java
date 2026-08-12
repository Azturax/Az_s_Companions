package com.azscompanions.event;

import com.azscompanions.entity.CompanionDimensionTravelSupport;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Bring companions along on any dimension change (vanilla + modded).
 * Distinct from logout park — {@link PlayerEvent.PlayerChangedDimensionEvent} is not disconnect.
 */
public final class CompanionDimensionTravelEvents {
    private CompanionDimensionTravelEvents() {
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompanionDimensionTravelSupport.followOwnerAcrossDimensions(
                player, event.getFrom(), event.getTo());
    }
}
