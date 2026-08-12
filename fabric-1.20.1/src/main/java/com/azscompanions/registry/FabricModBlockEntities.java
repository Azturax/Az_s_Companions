package com.azscompanions.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/** Registers Kon bed with vanilla bed block-entity support via reflection. */
public final class FabricModBlockEntities {
    private FabricModBlockEntities() {
    }

    public static void register() {
        addSupportedBlock(BlockEntityType.BED, FabricModBlocks.KON_BED);
    }

    @SuppressWarnings("unchecked")
    private static void addSupportedBlock(BlockEntityType<?> type, Block block) {
        try {
            for (Field field : BlockEntityType.class.getDeclaredFields()) {
                if (!Set.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                Object value = field.get(type);
                if (!(value instanceof Set<?> set) || set.isEmpty()) {
                    continue;
                }
                Object sample = set.iterator().next();
                if (!(sample instanceof Block)) {
                    continue;
                }
                Set<Block> mutable = new HashSet<>((Set<Block>) set);
                mutable.add(block);
                field.set(type, mutable);
                return;
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to register Kon bed with BlockEntityType.BED", e);
        }
    }
}
