package com.azscompanions.client.hud;

import com.azscompanions.AzsCompanions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Teamfight HUD — RenderGuiEvent graphics API moved in 26.2; draw hook deferred.
 */
@EventBusSubscriber(modid = AzsCompanions.MOD_ID, value = Dist.CLIENT)
public final class TeamFightHudEvents {
    private TeamFightHudEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Intentionally empty until RenderGuiEvent.Post graphics accessor is confirmed for 26.2.
    }
}
