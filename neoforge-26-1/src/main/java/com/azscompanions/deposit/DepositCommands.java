package com.azscompanions.deposit;

import com.azscompanions.data.ModTags;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code /deposit} and {@code /az deposit} — multi-select chests for gather deposit.
 */
public final class DepositCommands {
    private DepositCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> branch = buildBranch();
        dispatcher.register(Commands.literal("deposit")
                .executes(ctx -> enable(ctx.getSource().getPlayerOrException()))
                .then(Commands.literal("done").executes(ctx -> done(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource().getPlayerOrException()))));
        // /az deposit is attached from CompanionCommands via attachToRoot
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
        DepositSelectionSync.send(player, sel);
        player.sendSystemMessage(Component.translatable(
                "message.azscompanions.deposit_mode_on", sel.size()));
        return 1;
    }

    public static int done(ServerPlayer player) {
        DepositChestSelection sel = DepositChestSelection.of(player.getUUID());
        sel.finishKeepingSelection();
        DepositSelectionSync.send(player, sel);
        player.sendSystemMessage(Component.translatable(
                "message.azscompanions.deposit_done", sel.size()));
        return 1;
    }

    public static int clear(ServerPlayer player) {
        DepositChestSelection sel = DepositChestSelection.of(player.getUUID());
        sel.clearSelection();
        DepositSelectionSync.send(player, sel);
        player.sendSystemMessage(Component.translatable("message.azscompanions.deposit_cleared"));
        return 1;
    }

    public static boolean isDepositContainer(BlockState state) {
        return state != null && state.is(ModTags.Blocks.ALLOWED_CONTAINERS);
    }

    public static boolean tryToggleAt(ServerPlayer player, BlockPos pos) {
        DepositChestSelection sel = DepositChestSelection.of(player.getUUID());
        if (!sel.isSelecting()) {
            return false;
        }
        if (!isDepositContainer(player.level().getBlockState(pos))) {
            return false;
        }
        String dim = player.level().dimension().identifier().toString();
        boolean wasSelected = sel.contains(dim, pos.getX(), pos.getY(), pos.getZ());
        if (!wasSelected && sel.size() >= DepositChestSelection.MAX_CHESTS) {
            player.sendSystemMessage(Component.translatable(
                    "message.azscompanions.deposit_limit", DepositChestSelection.MAX_CHESTS));
            return true;
        }
        boolean nowSelected = sel.toggle(dim, pos.getX(), pos.getY(), pos.getZ());
        player.sendSystemMessage(Component.translatable(
                nowSelected ? "message.azscompanions.deposit_added" : "message.azscompanions.deposit_removed",
                pos.getX(), pos.getY(), pos.getZ(), sel.size()));
        DepositSelectionSync.send(player, sel);
        return true;
    }
}
