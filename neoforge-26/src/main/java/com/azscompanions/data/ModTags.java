package com.azscompanions.data;

import com.azscompanions.AzsCompanions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    private ModTags() {
    }

    public static final class Blocks {
        public static final TagKey<Block> HARVESTABLE_CROPS = block("harvestable_crops");
        public static final TagKey<Block> MACHINE_WORKSTATIONS = block("machine_workstations");
        public static final TagKey<Block> ALLOWED_CONTAINERS = block("allowed_containers");
        public static final TagKey<Block> BLACKLISTED_BLOCKS = block("blacklisted_blocks");
        public static final TagKey<Block> TASK_MATERIALS = block("task_materials");
        public static final TagKey<Block> GATHERABLE = block("gatherable");
        public static final TagKey<Block> COMPANION_SAFE = block("companion_safe");

        private Blocks() {
        }

        private static TagKey<Block> block(String path) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, path));
        }
    }

    public static final class Items {
        public static final TagKey<Item> REPLANTABLE_SEEDS = item("replantable_seeds");
        public static final TagKey<Item> COMPANION_TOOLS = item("companion_tools");
        public static final TagKey<Item> COMPANION_FOOD = item("companion_food");
        public static final TagKey<Item> COMPANION_ARMOR = item("companion_armor");
        public static final TagKey<Item> TASK_MATERIALS = item("task_materials");
        public static final TagKey<Item> ITEM_BLACKLIST = item("item_blacklist");

        private Items() {
        }

        private static TagKey<Item> item(String path) {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, path));
        }
    }

    public static final class EntityTypes {
        public static final TagKey<EntityType<?>> PROTECTED_ENTITIES = entity("protected_entities");

        private EntityTypes() {
        }

        private static TagKey<EntityType<?>> entity(String path) {
            return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, path));
        }
    }
}
