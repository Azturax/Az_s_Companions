package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanionsFabric;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

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
            if (!ClientPlayNetworking.canSend(FabricCciActionPacket.ID)) {
                AzsCompanionsFabric.LOGGER.debug("CCI action packet not yet registered on server");
                return;
            }
            FriendlyByteBuf buf = PacketByteBufs.create();
            FabricCciActionPacket.write(buf, new FabricCciActionPacket(action.name(), message == null ? "" : message));
            ClientPlayNetworking.send(FabricCciActionPacket.ID, buf);
        });
        AzsCompanionsFabric.LOGGER.info("CCI edition (Fabric) client bridge ready");
    }
}
