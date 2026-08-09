package com.koncompanions.registry;

import com.koncompanions.KonCompanions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** No custom block entities. Kon bed uses vanilla {@link BlockEntityType#BED}. */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, KonCompanions.MOD_ID);

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(ModBlockEntities::addBedBlocks);
    }

    private static void addBedBlocks(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.BED, ModBlocks.KON_BED.get());
    }
}
