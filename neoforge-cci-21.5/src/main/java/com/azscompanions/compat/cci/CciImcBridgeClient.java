package com.azscompanions.compat.cci;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client-only IMC poller. Kept in a separate class so dedicated servers never load client tick types.
 */
@OnlyIn(Dist.CLIENT)
public final class CciImcBridgeClient {
    private static int clientTickCounter;

    private CciImcBridgeClient() {
    }

    static void register() {
        NeoForge.EVENT_BUS.register(CciImcBridgeClient.class);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if ((++clientTickCounter % 5) != 0) {
            return;
        }
        CciImcBridge.drainQueuedMessages("client");
    }

    static void sendActionToServer(CciCompanionAction action, String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null) {
            CciImcBridge.sendToServer(action, message);
        }
    }
}
