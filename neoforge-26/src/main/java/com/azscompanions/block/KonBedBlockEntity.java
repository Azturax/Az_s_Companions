package com.azscompanions.block;

import com.azscompanions.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Block entity for {@link KonBedBlock} only — keeps vanilla {@code BlockEntityType.BED} untouched. */
public final class KonBedBlockEntity extends BlockEntity {
    public KonBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KON_BED.get(), pos, state);
    }
}
