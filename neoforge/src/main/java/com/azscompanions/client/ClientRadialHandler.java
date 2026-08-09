package com.azscompanions.client;

import com.azscompanions.client.screen.CompanionRadialScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = com.azscompanions.AzsCompanions.MOD_ID, value = Dist.CLIENT)
public final class ClientRadialHandler {
    private ClientRadialHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            // Consume presses while a screen is open so we don't queue opens.
            while (ModKeyMappings.OPEN_RADIAL.consumeClick()) {
                // no-op
            }
            return;
        }
        while (ModKeyMappings.OPEN_RADIAL.consumeClick()) {
            CompanionRadialScreen.openForOwnedCompanion();
        }
    }
}
