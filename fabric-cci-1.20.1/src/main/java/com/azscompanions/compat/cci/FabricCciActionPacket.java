package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanionsFabric;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record FabricCciActionPacket(String actionName, String message) {
    public static final ResourceLocation ID =
            new ResourceLocation(AzsCompanionsFabric.MOD_ID, "cci_action");

    public static void write(FriendlyByteBuf buf, FabricCciActionPacket p) {
        buf.writeUtf(p.actionName == null ? "" : p.actionName, 64);
        buf.writeUtf(p.message == null ? "" : p.message, 256);
    }

    public static FabricCciActionPacket read(FriendlyByteBuf buf) {
        return new FabricCciActionPacket(buf.readUtf(64), buf.readUtf(256));
    }

    public static void handle(ServerPlayer player, FabricCciActionPacket packet) {
        FabricCciCompanionAction action;
        try {
            action = FabricCciCompanionAction.valueOf(packet.actionName());
        } catch (IllegalArgumentException ex) {
            return;
        }
        FabricCciCompanionActions.applyOnServer(player, action, packet.message());
    }
}
