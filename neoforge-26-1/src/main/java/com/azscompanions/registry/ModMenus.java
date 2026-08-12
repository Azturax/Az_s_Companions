package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import com.azscompanions.menu.CompanionInventoryMenu;
import com.azscompanions.menu.CompanionManagementMenu;
import com.azscompanions.menu.CompanionSelectionMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AzsCompanions.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<CompanionSelectionMenu>> COMPANION_SELECTION =
            MENUS.register("companion_selection", () ->
                    IMenuTypeExtension.create(CompanionSelectionMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CompanionManagementMenu>> COMPANION_MANAGEMENT =
            MENUS.register("companion_management", () ->
                    IMenuTypeExtension.create(CompanionManagementMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CompanionInventoryMenu>> COMPANION_INVENTORY =
            MENUS.register("companion_inventory", () ->
                    IMenuTypeExtension.create(CompanionInventoryMenu::new));

    private ModMenus() {
    }
}
