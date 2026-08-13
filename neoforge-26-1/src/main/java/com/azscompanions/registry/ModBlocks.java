package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import com.azscompanions.block.KonBedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AzsCompanions.MOD_ID);

    /** Must use {@code registerBlock} so NeoForge sets the block id on Properties (MC 26+). */
    public static final DeferredBlock<KonBedBlock> KON_BED = BLOCKS.registerBlock(
            "kon_bed",
            KonBedBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.2F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
                    .noOcclusion());

    private ModBlocks() {
    }
}
