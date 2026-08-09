package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import com.azscompanions.item.CompanionCharmItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AzsCompanions.MOD_ID);

    /** Loot-only charm: binds to one Kon and toggles summon/despawn. */
    public static final DeferredItem<CompanionCharmItem> COMPANION_CHARM =
            ITEMS.register("companion_charm", () ->
                    new CompanionCharmItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> KON_BED =
            ITEMS.register("kon_bed", () ->
                    new BlockItem(ModBlocks.KON_BED.get(), new Item.Properties().stacksTo(1)));

    private ModItems() {
    }
}
