package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import com.azscompanions.item.CompanionCharmItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AzsCompanions.MOD_ID);

    public static final RegistryObject<CompanionCharmItem> COMPANION_CHARM =
            ITEMS.register("companion_charm", () ->
                    new CompanionCharmItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BlockItem> KON_BED =
            ITEMS.register("kon_bed", () ->
                    new BlockItem(ModBlocks.KON_BED.get(), new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
