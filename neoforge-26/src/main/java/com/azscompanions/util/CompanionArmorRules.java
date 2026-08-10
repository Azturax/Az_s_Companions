package com.azscompanions.util;

import com.azscompanions.entity.CompanionForm;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/** Inventory / equipment rules so armor is never invisible on unsupported forms. */
public final class CompanionArmorRules {
    private CompanionArmorRules() {
    }

    public static boolean isCanineArmor(ItemStack stack) {
        return stack.getItem() instanceof AnimalArmorItem animal
                && animal.getBodyType() == AnimalArmorItem.BodyType.CANINE;
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
            return stack.getItem() instanceof ArmorItem armor && armor.getType().getSlot() == uiSlot;
        }
        if (form.supportsWolfArmor() && uiSlot == EquipmentSlot.CHEST) {
            return isCanineArmor(stack);
        }
        return false;
    }
}
