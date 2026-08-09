package com.koncompanions.registry;

import com.koncompanions.KonCompanions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KonCompanions.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.koncompanions"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.COMPANION_CHARM.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.COMPANION_CHARM.get());
                        output.accept(ModItems.KON_BED.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
