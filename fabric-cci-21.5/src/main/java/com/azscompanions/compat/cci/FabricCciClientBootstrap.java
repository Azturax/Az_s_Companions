package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanionsFabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/**
 * Client-only CCI wiring so dedicated servers never load Minecraft client types from the bridge.
 */
public final class FabricCciClientBootstrap implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FabricCciBridge.setClientSender((action, message) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.getConnection() == null) {
                return;
            }
            if (!ClientPlayNetworking.canSend(FabricCciActionPacket.TYPE)) {
                AzsCompanionsFabric.LOGGER.debug("CCI action packet not yet registered on server");
                return;
            }
            ClientPlayNetworking.send(new FabricCciActionPacket(action.name(), message == null ? "" : message));
        });
        AzsCompanionsFabric.LOGGER.info("CCI edition (Fabric) client bridge ready");
    }
}
