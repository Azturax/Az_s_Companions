package com.azscompanions.registry;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.block.FabricKonBedBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class FabricModBlocks {
    public static Block KON_BED;

    private FabricModBlocks() {
    }

    public static void register() {
        KON_BED = Registry.register(
                BuiltInRegistries.BLOCK,
                id("kon_bed"),
                new FabricKonBedBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_PINK)
                        .strength(0.2F)
                        .sound(SoundType.WOOD)
                        .ignitedByLava()
                        .pushReaction(PushReaction.DESTROY)
                        .noOcclusion()));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(AzsCompanionsFabric.MOD_ID, path);
    }
}
