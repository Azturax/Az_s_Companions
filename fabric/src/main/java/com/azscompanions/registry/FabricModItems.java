package com.azscompanions.registry;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.entity.JindujunSupport;
import com.azscompanions.item.FabricCompanionCharmItem;
import com.azscompanions.item.FabricJindujunWhistleItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class FabricModItems {
    public static Item COMPANION_CHARM;
    public static Item KON_BED;
    public static Item JINDUJUN_WHISTLE;

    private FabricModItems() {
    }

    public static void register() {
        COMPANION_CHARM = Registry.register(
                BuiltInRegistries.ITEM,
                id("companion_charm"),
                new FabricCompanionCharmItem(new Item.Properties().stacksTo(1)));
        KON_BED = Registry.register(
                BuiltInRegistries.ITEM,
                id("kon_bed"),
                new BlockItem(FabricModBlocks.KON_BED, new Item.Properties().stacksTo(1)));
        JINDUJUN_WHISTLE = Registry.register(
                BuiltInRegistries.ITEM,
                id(JindujunSupport.ITEM_ID),
                new FabricJindujunWhistleItem(new Item.Properties().stacksTo(1)));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> {
            entries.accept(COMPANION_CHARM);
            entries.accept(KON_BED);
            entries.accept(JINDUJUN_WHISTLE);
        });
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, path);
    }
}
