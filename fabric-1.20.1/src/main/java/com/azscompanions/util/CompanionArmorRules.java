package com.azscompanions.util;

import com.azscompanions.entity.CompanionForm;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/** Inventory / equipment rules. Wolf body armor item type is 1.20.5+ — omitted on 1.20.1. */
public final class CompanionArmorRules {
    private CompanionArmorRules() {
    }

    public static boolean isCanineArmor(ItemStack stack) {
        return false;
    }

    public static boolean isWolfBodyArmor(ItemStack stack) {
        return isCanineArmor(stack);
    }

    public static boolean mayPlaceInArmorSlot(CompanionForm form, EquipmentSlot uiSlot, ItemStack stack) {
        if (stack.isEmpty() || form == null) {
            return false;
        }
        if (form.supportsHumanoidArmor()) {
            return stack.getItem() instanceof ArmorItem armor && armor.getType().getSlot() == uiSlot;
        }
        return false;
    }
}
