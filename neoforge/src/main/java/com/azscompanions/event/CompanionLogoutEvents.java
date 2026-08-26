package com.azscompanions.event;

import com.azscompanions.entity.CompanionLogoutSupport;
import com.azscompanions.perk.WigglyDogPerk;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Park companions on disconnect only; restore near the player on join.
 * Dimension travel uses {@link com.azscompanions.event.CompanionDimensionTravelEvents} instead.
 */
public final class CompanionLogoutEvents {
    private CompanionLogoutEvents() {
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompanionLogoutSupport.parkOwnedCompanions(player);
            WigglyDogPerk.parkFor(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompanionLogoutSupport.restoreParkedCompanions(player);
        }
    }
}
