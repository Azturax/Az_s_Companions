package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import com.azscompanions.block.KonBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AzsCompanions.MOD_ID);

    public static final RegistryObject<KonBedBlock> KON_BED = BLOCKS.register("kon_bed", () ->
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
