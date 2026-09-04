package com.azscompanions.util;

import com.azscompanions.data.ModTags;
import com.azscompanions.entity.CompanionEquipmentSupport;
import com.azscompanions.entity.CompanionForm;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Inventory / equipment rules so armor is never invisible on unsupported forms. */
public final class CompanionArmorRules {
    private CompanionArmorRules() {
    }

    public static boolean isCanineArmor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof AnimalArmorItem animal
                && animal.getBodyType() == AnimalArmorItem.BodyType.CANINE) {
            return true;
        }
        return CompanionEquipmentSupport.looksLikeBodyArmor(itemId(stack));
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
        EquipmentSlot declared = stack.getEquipmentSlot();
        if (declared == uiSlot) {
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
            case HEAD -> stack.is(ItemTags.HEAD_ARMOR)
                    || stack.is(itemTag("c", "armors/helmets"))
                    || stack.is(itemTag("forge", "armors/helmets"))
                    || stack.is(itemTag("c", "armors/helmet"));
            case CHEST -> stack.is(ItemTags.CHEST_ARMOR)
                    || stack.is(itemTag("c", "armors/chestplates"))
                    || stack.is(itemTag("forge", "armors/chestplates"));
            case LEGS -> stack.is(ItemTags.LEG_ARMOR)
                    || stack.is(itemTag("c", "armors/leggings"))
                    || stack.is(itemTag("forge", "armors/leggings"));
            case FEET -> stack.is(ItemTags.FOOT_ARMOR)
                    || stack.is(itemTag("c", "armors/boots"))
                    || stack.is(itemTag("forge", "armors/boots"));
            default -> false;
        };
    }

    private static boolean isGenericArmor(ItemStack stack) {
        return stack.is(ModTags.Items.COMPANION_ARMOR)
                || stack.is(itemTag("c", "armors"))
                || stack.is(itemTag("forge", "armors"))
                || stack.is(itemTag("neoforge", "armors"));
    }

    private static boolean hasArmorAttributesForSlot(ItemStack stack, EquipmentSlot uiSlot) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(
                DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!entry.slot().test(uiSlot)) {
                continue;
            }
            String path = entry.attribute().unwrapKey()
                    .map(key -> key.location().getPath())
                    .orElse("");
            if (path.endsWith("armor") || path.endsWith("armor_toughness")) {
                return true;
            }
        }
        return false;
    }

    private static TagKey<Item> itemTag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
