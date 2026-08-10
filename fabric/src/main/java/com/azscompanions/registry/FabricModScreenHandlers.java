package com.azscompanions.registry;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.menu.FabricCompanionInventoryMenu;
import com.azscompanions.menu.FabricCompanionSelectionMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class FabricModScreenHandlers {
    public static MenuType<FabricCompanionSelectionMenu> SELECTION;
    public static MenuType<FabricCompanionInventoryMenu> INVENTORY;

    private FabricModScreenHandlers() {
    }

    public static void register() {
        SELECTION = Registry.register(
                BuiltInRegistries.MENU,
                id("companion_selection"),
                new MenuType<>(FabricCompanionSelectionMenu::new, FeatureFlags.VANILLA_SET));
        INVENTORY = Registry.register(
                BuiltInRegistries.MENU,
                id("companion_inventory"),
                new ExtendedScreenHandlerType<>(
                        (syncId, inv, entityId) -> new FabricCompanionInventoryMenu(syncId, inv, entityId),
                        net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, path);
    }
}
