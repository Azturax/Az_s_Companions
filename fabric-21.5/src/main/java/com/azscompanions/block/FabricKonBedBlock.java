package com.azscompanions.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class FabricKonBedBlock extends BedBlock {
    public FabricKonBedBlock(BlockBehaviour.Properties properties) {
        super(DyeColor.PINK, properties);
    }
}
