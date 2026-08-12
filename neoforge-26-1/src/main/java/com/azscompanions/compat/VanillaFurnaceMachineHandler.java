package com.azscompanions.compat;

import com.azscompanions.api.MachineHandler;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public final class VanillaFurnaceMachineHandler implements MachineHandler {
    @Override
    public boolean canHandle(ServerLevel level, BlockPos pos, CompanionEntity companion) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER);
    }

    @Override
    public Result interact(ServerLevel level, BlockPos pos, CompanionEntity companion) {
        // Capability insert deferred for NeoForge 26.2 — furnace fueling disabled until ported.
        return Result.FAILED;
    }
}
