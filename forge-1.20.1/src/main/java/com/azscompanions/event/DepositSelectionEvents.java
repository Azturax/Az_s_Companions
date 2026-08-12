package com.azscompanions.event;

import com.azscompanions.deposit.DepositChestSelection;
import com.azscompanions.deposit.DepositCommands;
import com.azscompanions.deposit.DepositSelectionSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

/** Deposit chest multi-select: right-click toggle while selection mode is on. */
public final class DepositSelectionEvents {
    private DepositSelectionEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!DepositChestSelection.of(player.getUUID()).isSelecting()) {
            return;
        }
        if (!DepositCommands.tryToggleAt(player, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DepositChestSelection sel = DepositChestSelection.of(player.getUUID());
        if (sel.isSelecting()) {
            sel.finishKeepingSelection();
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DepositSelectionSync.send(player, DepositChestSelection.of(player.getUUID()));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DepositChestSelection.clearAll();
    }
}
