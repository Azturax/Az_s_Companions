package com.azscompanions.util;

import com.azscompanions.entity.CompanionEquipmentSupport;
import com.azscompanions.entity.CompanionForm;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Inventory / equipment rules. Wolf body armor item type is 1.20.5+ — omitted on 1.20.1. */
public final class CompanionArmorRules {
    private CompanionArmorRules() {
    }

    public static boolean isCanineArmor(ItemStack stack) {
        return !stack.isEmpty() && CompanionEquipmentSupport.looksLikeBodyArmor(itemId(stack));
    }

    public static boolean isWolfBodyArmor(ItemStack stack) {
        return isCanineArmor(stack);
    }

    public static boolean mayPlaceInArmorSlot(CompanionForm form, EquipmentSlot uiSlot, ItemStack stack) {
        if (stack.isEmpty() || form == null) {
            return false;
        }
        if (form.supportsHumanoidArmor()) {
            if (isCanineArmor(stack)) {
                return false;
            }
            return isArmorForSlot(stack, uiSlot);
        }
        if (form.supportsWolfArmor() && uiSlot == EquipmentSlot.CHEST) {
            return isCanineArmor(stack);
        }
        return false;
    }

    public static boolean isArmorForSlot(ItemStack stack, EquipmentSlot uiSlot) {
        if (stack.isEmpty() || uiSlot == null) {
            return false;
        }
        if (stack.getItem() instanceof ArmorItem armor && armor.getType().getSlot() == uiSlot) {
            return true;
        }
        if (stack.getItem() instanceof Equipable equipable && equipable.getEquipmentSlot() == uiSlot) {
            return true;
        }
        if (taggedForSlot(stack, uiSlot)) {
            return true;
        }
        if (hasArmorAttributesForSlot(stack, uiSlot)) {
            return true;
        }
        String id = itemId(stack);
        if (CompanionEquipmentSupport.matchesArmorSlot(id, CompanionEquipmentSupport.slotName(uiSlot.name()))) {
            return true;
        }
        return isGenericArmor(stack) && CompanionEquipmentSupport.armorKind(id) == CompanionEquipmentSupport.ArmorKind.NONE;
    }

    private static boolean taggedForSlot(ItemStack stack, EquipmentSlot uiSlot) {
        return switch (uiSlot) {
            case HEAD -> stack.is(itemTag("c", "armors/helmets")) || stack.is(itemTag("c", "armors/helmet"));
            case CHEST -> stack.is(itemTag("c", "armors/chestplates"));
            case LEGS -> stack.is(itemTag("c", "armors/leggings"));
            case FEET -> stack.is(itemTag("c", "armors/boots"));
            default -> false;
        };
    }

    private static boolean isGenericArmor(ItemStack stack) {
        return stack.is(itemTag("azscompanions", "companion_armor")) || stack.is(itemTag("c", "armors"));
    }

    private static boolean hasArmorAttributesForSlot(ItemStack stack, EquipmentSlot uiSlot) {
        var mods = stack.getAttributeModifiers(uiSlot);
        return mods.containsKey(Attributes.ARMOR) || mods.containsKey(Attributes.ARMOR_TOUGHNESS);
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(namespace, path));
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
