package com.azscompanions.util;

import com.azscompanions.data.ModTags;
import com.azscompanions.entity.CompanionEquipmentSupport;
import com.azscompanions.entity.CompanionForm;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

/** Armor classification for NeoForge 26.1 — tags, equippable component, and id heuristics. */
public final class CompanionArmorRules {
    private CompanionArmorRules() {
    }

    public static boolean isCanineArmor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot() == EquipmentSlot.BODY) {
            return true;
        }
        return CompanionEquipmentSupport.looksLikeBodyArmor(itemId(stack));
    }

    public static boolean isArmorForSlot(ItemStack stack, EquipmentSlot uiSlot) {
        if (stack.isEmpty() || uiSlot == null) {
            return false;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot() == uiSlot) {
            return true;
        }
        if (taggedForSlot(stack, uiSlot)) {
            return true;
        }
        String id = itemId(stack);
        if (CompanionEquipmentSupport.matchesArmorSlot(id, CompanionEquipmentSupport.slotName(uiSlot.name()))) {
            return true;
        }
        return isGenericArmor(stack) && CompanionEquipmentSupport.armorKind(id) == CompanionEquipmentSupport.ArmorKind.NONE;
    }

    public static boolean mayPlaceInArmorSlot(CompanionForm form, EquipmentSlot uiSlot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        if (form != null && form.supportsHumanoidArmor()) {
            if (isCanineArmor(stack)) {
                return false;
            }
            return isArmorForSlot(stack, uiSlot);
        }
        if (form != null && form.supportsWolfArmor() && (uiSlot == EquipmentSlot.CHEST || uiSlot == EquipmentSlot.BODY)) {
            return isCanineArmor(stack);
        }
        return isArmorForSlot(stack, uiSlot);
    }

    private static boolean taggedForSlot(ItemStack stack, EquipmentSlot uiSlot) {
        return switch (uiSlot) {
            case HEAD -> stack.is(ItemTags.HEAD_ARMOR) || stack.is(itemTag("c", "armors/helmets"));
            case CHEST -> stack.is(ItemTags.CHEST_ARMOR) || stack.is(itemTag("c", "armors/chestplates"));
            case LEGS -> stack.is(ItemTags.LEG_ARMOR) || stack.is(itemTag("c", "armors/leggings"));
            case FEET -> stack.is(ItemTags.FOOT_ARMOR) || stack.is(itemTag("c", "armors/boots"));
            default -> false;
        };
    }

    private static boolean isGenericArmor(ItemStack stack) {
        return stack.is(ModTags.Items.COMPANION_ARMOR) || stack.is(itemTag("c", "armors"));
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path));
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
