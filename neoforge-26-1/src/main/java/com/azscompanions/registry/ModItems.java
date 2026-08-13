package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import com.azscompanions.item.CompanionCharmItem;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AzsCompanions.MOD_ID);

    /** Loot-only charm: binds to one Kon and toggles summon/despawn. */
    public static final DeferredItem<CompanionCharmItem> COMPANION_CHARM =
            ITEMS.registerItem("companion_charm", CompanionCharmItem::new, props -> props.stacksTo(1));

    /** BlockItem via holder — do not call {@code ModBlocks.KON_BED.get()} during item registration. */
    public static final DeferredItem<BlockItem> KON_BED =
            ITEMS.registerSimpleBlockItem(ModBlocks.KON_BED, props -> props.stacksTo(1));

    private ModItems() {
    }
}
