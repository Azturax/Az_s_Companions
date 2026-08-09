package com.koncompanions.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Kon-themed bed: vanilla sleep/spawn point, painted daki+pillow on the bed texture,
 * and companion home / night-sleep target.
 */
public final class KonBedBlock extends BedBlock {
    public KonBedBlock(BlockBehaviour.Properties properties) {
        super(DyeColor.PINK, properties);
    }
}
