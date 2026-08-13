package com.azscompanions.compat.cci;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Forge 1.20.1 SimpleChannel packet for CCI IMC → server actions. */
public record CciActionPacket(String actionName, String message) {
    public static void encode(CciActionPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.actionName() == null ? "" : packet.actionName(), 64);
        buf.writeUtf(packet.message() == null ? "" : packet.message(), 256);
    }

    public static CciActionPacket decode(FriendlyByteBuf buf) {
        return new CciActionPacket(buf.readUtf(64), buf.readUtf(256));
    }

    public static void handle(CciActionPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            CciCompanionAction action;
            try {
                action = CciCompanionAction.valueOf(packet.actionName());
            } catch (IllegalArgumentException ex) {
                return;
            }
            CciCompanionActions.applyOnServer(player, action, packet.message());
        });
        ctx.setPacketHandled(true);
    }
}
