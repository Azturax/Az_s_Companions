package com.azscompanions.deposit;

import com.azscompanions.network.packet.DepositSelectionSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** NeoForge helper: push deposit selection snapshot to the owning player. */
public final class DepositSelectionSync {
    private DepositSelectionSync() {
    }

    public static void send(ServerPlayer player, DepositChestSelection selection) {
        if (player == null || selection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player,
                new DepositSelectionSyncPacket(selection.snapshot().encode()));
    }
}
