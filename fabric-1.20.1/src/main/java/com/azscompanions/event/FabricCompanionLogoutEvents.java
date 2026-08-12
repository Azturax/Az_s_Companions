package com.azscompanions.event;

import com.azscompanions.entity.FabricCompanionLogoutSupport;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Park companions on disconnect only; restore near the player on join.
 * Dimension travel uses {@link FabricCompanionDimensionTravelEvents} instead.
 */
public final class FabricCompanionLogoutEvents {
    private FabricCompanionLogoutEvents() {
    }

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                FabricCompanionLogoutSupport.parkOwnedCompanions(handler.getPlayer()));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                FabricCompanionLogoutSupport.restoreParkedCompanions(handler.getPlayer()));
    }
}
