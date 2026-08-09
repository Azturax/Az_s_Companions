package com.koncompanions.registry;

import com.koncompanions.KonCompanions;
import com.koncompanions.block.KonBedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KonCompanions.MOD_ID);

    public static final DeferredBlock<KonBedBlock> KON_BED = BLOCKS.register("kon_bed", () ->
            new KonBedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PINK)
                    .strength(0.2F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)
                    .noOcclusion()));

    private ModBlocks() {
    }
}
