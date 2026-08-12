package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import com.azscompanions.menu.CompanionInventoryMenu;
import com.azscompanions.menu.CompanionManagementMenu;
import com.azscompanions.menu.CompanionSelectionMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AzsCompanions.MOD_ID);

    public static final RegistryObject<MenuType<CompanionSelectionMenu>> COMPANION_SELECTION =
            MENUS.register("companion_selection", () ->
                    IForgeMenuType.create(CompanionSelectionMenu::new));

    public static final RegistryObject<MenuType<CompanionManagementMenu>> COMPANION_MANAGEMENT =
            MENUS.register("companion_management", () ->
                    IForgeMenuType.create(CompanionManagementMenu::new));

    public static final RegistryObject<MenuType<CompanionInventoryMenu>> COMPANION_INVENTORY =
            MENUS.register("companion_inventory", () ->
                    IForgeMenuType.create(CompanionInventoryMenu::new));

    private ModMenus() {
    }
}
