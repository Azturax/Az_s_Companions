package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AzsCompanions.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.azscompanions"))
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
