package com.azscompanions.data;

import com.azscompanions.AzsCompanionsFabric;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Fabric block tags used by deposit selection / tasks. */
public final class FabricModTags {
    public static final TagKey<Block> ALLOWED_CONTAINERS = TagKey.create(
            Registries.BLOCK,
            new ResourceLocation(AzsCompanionsFabric.MOD_ID, "allowed_containers"));

    private FabricModTags() {
    }
}
