package com.azscompanions.compat.cci;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Client-only IMC poller. Kept separate so dedicated servers never load client tick types.
 */
@OnlyIn(Dist.CLIENT)
public final class CciImcBridgeClient {
    private static int clientTickCounter;

    private CciImcBridgeClient() {
    }

    static void register() {
        MinecraftForge.EVENT_BUS.register(CciImcBridgeClient.class);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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
