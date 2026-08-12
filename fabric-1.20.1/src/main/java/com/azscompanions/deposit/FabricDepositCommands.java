package com.azscompanions.deposit;

import com.azscompanions.data.FabricModTags;
import com.azscompanions.network.FabricNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/** Fabric {@code /deposit} and {@code /az deposit}. */
public final class FabricDepositCommands {
    private FabricDepositCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deposit")
                .executes(ctx -> enable(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("done").executes(ctx -> done(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource().getPlayerOrException()))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> buildBranch() {
        return Commands.literal("deposit")
                .executes(ctx -> enable(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("done").executes(ctx -> done(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource().getPlayerOrException())));
    }

    public static int enable(ServerPlayer player) {
        DepositChestSelection sel = DepositChestSelection.of(player.getUUID());
        sel.enableSelecting();
        sync(player, sel);
        player.displayClientMessage(Component.translatable(
                "message.azscompanions.deposit_mode_on", sel.size()), true);
        return 1;
    }

    public static int done(ServerPlayer player) {
        DepositChestSelection sel = DepositChestSelection.of(player.getUUID());
        sel.finishKeepingSelection();
        sync(player, sel);
        player.displayClientMessage(Component.translatable(
                "message.azscompanions.deposit_done", sel.size()), true);
        return 1;
    }

    public static int clear(ServerPlayer player) {
        DepositChestSelection sel = DepositChestSelection.of(player.getUUID());
        sel.clearSelection();
        sync(player, sel);
        player.displayClientMessage(Component.translatable("message.azscompanions.deposit_cleared"), true);
        return 1;
    }

    public static void sync(ServerPlayer player, DepositChestSelection sel) {
        FabricNetworking.sendToPlayer(player, FabricNetworking.DepositSelectionPayload.ID, buf -> FabricNetworking.DepositSelectionPayload.write(buf, new FabricNetworking.DepositSelectionPayload(sel.snapshot().encode())));
    }

    public static boolean isDepositContainer(BlockState state) {
        return state != null && state.is(FabricModTags.ALLOWED_CONTAINERS);
    }

    public static boolean tryToggleAt(ServerPlayer player, BlockPos pos) {
        DepositChestSelection sel = DepositChestSelection.of(player.getUUID());
        if (!sel.isSelecting()) {
            return false;
        }
        if (!isDepositContainer(player.level().getBlockState(pos))) {
            return false;
        }
        String dim = player.level().dimension().location().toString();
        boolean wasSelected = sel.contains(dim, pos.getX(), pos.getY(), pos.getZ());
        if (!wasSelected && sel.size() >= DepositChestSelection.MAX_CHESTS) {
            player.displayClientMessage(Component.translatable(
                    "message.azscompanions.deposit_limit", DepositChestSelection.MAX_CHESTS), true);
            return true;
        }
        boolean nowSelected = sel.toggle(dim, pos.getX(), pos.getY(), pos.getZ());
        player.displayClientMessage(Component.translatable(
                nowSelected ? "message.azscompanions.deposit_added" : "message.azscompanions.deposit_removed",
                pos.getX(), pos.getY(), pos.getZ(), sel.size()), true);
        sync(player, sel);
        return true;
    }
}
