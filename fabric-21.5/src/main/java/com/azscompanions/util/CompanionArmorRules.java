package com.azscompanions.util;

import com.azscompanions.entity.CompanionForm;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;

/** Inventory / equipment rules so armor is never invisible on unsupported forms. */
public final class CompanionArmorRules {
    private CompanionArmorRules() {
    }

    public static boolean isCanineArmor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(Items.WOLF_ARMOR)) {
            return true;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.BODY;
    }

    /**
     * Whether {@code stack} may go in the companion inventory armor column slot for {@code uiSlot}
     * (HEAD/CHEST/LEGS/FEET). Wolf armor is stored in the CHEST UI slot and exposed as BODY for rendering.
     */
    public static boolean mayPlaceInArmorSlot(CompanionForm form, EquipmentSlot uiSlot, ItemStack stack) {
        if (stack.isEmpty() || form == null) {
            return false;
        }
        if (form.supportsHumanoidArmor()) {
            if (isCanineArmor(stack)) {
                return false;
            }
            Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
            return equippable != null && equippable.slot() == uiSlot;
        }
        if (form.supportsWolfArmor() && uiSlot == EquipmentSlot.CHEST) {
            return isCanineArmor(stack);
        }
        return false;
    }
}
