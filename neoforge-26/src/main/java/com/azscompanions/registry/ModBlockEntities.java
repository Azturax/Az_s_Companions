package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import com.azscompanions.block.KonBedBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Kon bed uses its own BE type so vanilla beds keep BlockEntityType.BED + BedRenderer. */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AzsCompanions.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KonBedBlockEntity>> KON_BED =
            BLOCK_ENTITIES.register("kon_bed", () ->
                    new BlockEntityType<>(KonBedBlockEntity::new, ModBlocks.KON_BED.get()));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
