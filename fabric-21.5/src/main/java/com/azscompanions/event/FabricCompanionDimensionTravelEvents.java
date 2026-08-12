package com.azscompanions.event;

import com.azscompanions.entity.FabricCompanionDimensionTravelSupport;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;

/**
 * Bring companions along on any dimension change (vanilla + modded).
 * Distinct from logout park — world-change is not disconnect.
 */
public final class FabricCompanionDimensionTravelEvents {
    private FabricCompanionDimensionTravelEvents() {
    }

    public static void register() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                FabricCompanionDimensionTravelSupport.followOwnerAcrossDimensions(
                        player, origin.dimension(), destination.dimension()));
    }
}
