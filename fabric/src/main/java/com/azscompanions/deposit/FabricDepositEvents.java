package com.azscompanions.deposit;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

/** Fabric deposit chest multi-select via right-click while selection mode is on. */
public final class FabricDepositEvents {
    private FabricDepositEvents() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (!DepositChestSelection.of(serverPlayer.getUUID()).isSelecting()) {
                return InteractionResult.PASS;
            }
            if (!FabricDepositCommands.tryToggleAt(serverPlayer, hitResult.getBlockPos())) {
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                FabricDepositCommands.sync(handler.getPlayer(),
                        DepositChestSelection.of(handler.getPlayer().getUUID())));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            DepositChestSelection sel = DepositChestSelection.of(handler.getPlayer().getUUID());
            if (sel.isSelecting()) {
                sel.finishKeepingSelection();
            }
        });
    }
}
