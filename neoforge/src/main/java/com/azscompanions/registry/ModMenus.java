package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import com.azscompanions.menu.CompanionInventoryMenu;
import com.azscompanions.menu.CompanionManagementMenu;
import com.azscompanions.menu.CompanionSelectionMenu;
import com.azscompanions.menu.RadialCommandMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menus kept for command/legacy packet compatibility; interact entry points are disabled. */
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

    public static final DeferredHolder<MenuType<?>, MenuType<RadialCommandMenu>> RADIAL_COMMAND =
            MENUS.register("radial_command", () ->
                    IMenuTypeExtension.create(RadialCommandMenu::new));

    private ModMenus() {
    }
}
