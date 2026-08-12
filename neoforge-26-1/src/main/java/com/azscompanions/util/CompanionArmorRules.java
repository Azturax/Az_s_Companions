package com.azscompanions.util;

import com.azscompanions.entity.CompanionForm;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Armor classification for NeoForge 26.2 (simplified while ArmorItem APIs settle). */
public final class CompanionArmorRules {
    private CompanionArmorRules() {
    }

    public static boolean isCanineArmor(ItemStack stack) {
        return false;
    }

    public static boolean isArmorForSlot(ItemStack stack, EquipmentSlot uiSlot) {
        return !stack.isEmpty();
    }

    public static boolean mayPlaceInArmorSlot(CompanionForm form, EquipmentSlot uiSlot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        if (form != null && !form.isPlayer() && uiSlot == EquipmentSlot.BODY) {
            return isCanineArmor(stack) || isArmorForSlot(stack, uiSlot);
        }
        return isArmorForSlot(stack, uiSlot);
    }
}
