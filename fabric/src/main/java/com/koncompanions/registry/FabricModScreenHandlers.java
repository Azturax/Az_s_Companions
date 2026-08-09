package com.koncompanions.registry;

import com.koncompanions.KonCompanionsFabric;
import com.koncompanions.menu.FabricCompanionInventoryMenu;
import com.koncompanions.menu.FabricCompanionSelectionMenu;
import com.koncompanions.menu.FabricRadialCommandMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class FabricModScreenHandlers {
    public static MenuType<FabricCompanionSelectionMenu> SELECTION;
    public static MenuType<FabricCompanionInventoryMenu> INVENTORY;
    public static MenuType<FabricRadialCommandMenu> RADIAL;

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
        RADIAL = Registry.register(
                BuiltInRegistries.MENU,
                id("radial_command"),
                new ExtendedScreenHandlerType<>(
                        (syncId, inv, entityId) -> new FabricRadialCommandMenu(syncId, inv, entityId),
                        net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(KonCompanionsFabric.MOD_ID, path);
    }
}
