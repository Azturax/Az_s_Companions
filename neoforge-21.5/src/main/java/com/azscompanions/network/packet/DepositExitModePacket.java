package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.deposit.DepositChestSelection;
import com.azscompanions.deposit.DepositSelectionSync;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: exit deposit selection mode (Esc / client), keep selected chests. */
public record DepositExitModePacket() implements CustomPacketPayload {
    public static final Type<DepositExitModePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "deposit_exit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositExitModePacket> STREAM_CODEC =
            StreamCodec.unit(new DepositExitModePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DepositExitModePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
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
    }
}
