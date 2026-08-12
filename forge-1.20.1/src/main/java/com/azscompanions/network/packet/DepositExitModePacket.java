package com.azscompanions.network.packet;

import com.azscompanions.deposit.DepositChestSelection;
import com.azscompanions.deposit.DepositSelectionSync;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** C2S: exit deposit selection mode (Esc / client), keep selected chests. */
public record DepositExitModePacket() {
    public static void encode(DepositExitModePacket packet, FriendlyByteBuf buf) {
    }

    public static DepositExitModePacket decode(FriendlyByteBuf buf) {
        return new DepositExitModePacket();
    }


    public static void handle(DepositExitModePacket packet, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            DepositChestSelection sel = DepositChestSelection.of(player.getUUID());
            if (!sel.isSelecting()) {
                return;
            }
            sel.finishKeepingSelection();
            DepositSelectionSync.send(player, sel);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.azscompanions.deposit_done", sel.size()), true);
        });
        context.setPacketHandled(true);

    }
}
