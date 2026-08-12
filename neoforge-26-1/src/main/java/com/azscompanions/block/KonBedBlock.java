package com.azscompanions.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Kon-themed bed: vanilla sleep/spawn point, custom sheet via KonBedBlockEntity,
 * and companion home / night-sleep target.
 */
public final class KonBedBlock extends BedBlock implements EntityBlock {
    public KonBedBlock(BlockBehaviour.Properties properties) {
        super(DyeColor.PINK, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KonBedBlockEntity(pos, state);
    }
}
