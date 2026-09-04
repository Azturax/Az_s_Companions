package com.azscompanions.entity;

import java.util.Locale;

/**
 * Loader-agnostic equipment heuristics so companions accept modded armor/tools
 * that do not extend vanilla {@code ArmorItem}/{@code SwordItem}.
 * <p>
 * Permissive: when an id looks wearable or tool-like, allow it. Dirt, food, and
 * other junk without an equipment-shaped path are rejected by callers unless the
 * item has a real equippable component, slot tag, or armor attribute.
 */
public final class CompanionEquipmentSupport {
    public enum ArmorKind {
        NONE,
        HEAD,
        CHEST,
        LEGS,
        FEET,
        BODY,
        ANY
    }

    private CompanionEquipmentSupport() {
    }

    public static String normalizeId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "";
        }
        String id = itemId.trim().toLowerCase(Locale.ROOT);
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        return id;
    }

    public static String pathOf(String itemId) {
        String id = normalizeId(itemId);
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    public static boolean looksLikeArmor(String itemId) {
        return armorKind(itemId) != ArmorKind.NONE;
    }

    /**
     * {@code slotName} is {@code head}/{@code chest}/{@code legs}/{@code feet}/{@code body}.
     */
    public static boolean matchesArmorSlot(String itemId, String slotName) {
        if (slotName == null || slotName.isBlank()) {
            return false;
        }
        String slot = slotName.trim().toLowerCase(Locale.ROOT);
        ArmorKind kind = armorKind(itemId);
        return switch (kind) {
            case NONE -> false;
            case ANY -> isHumanoidArmorSlot(slot);
            case BODY -> "chest".equals(slot) || "body".equals(slot);
            case HEAD -> "head".equals(slot);
            case CHEST -> "chest".equals(slot);
            case LEGS -> "legs".equals(slot);
            case FEET -> "feet".equals(slot);
        };
    }

    public static boolean looksLikeBodyArmor(String itemId) {
        return armorKind(itemId) == ArmorKind.BODY;
    }

    public static boolean looksLikeTool(String itemId) {
        String path = pathOf(itemId);
        if (path.isEmpty()) {
            return false;
        }
        return hasEndToken(path, "pickaxe")
                || hasEndToken(path, "shovel")
                || hasEndToken(path, "spade")
                || hasEndToken(path, "hoe")
                || hasEndToken(path, "paxel")
                || hasEndToken(path, "drill")
                || hasEndToken(path, "excavator")
                || hasEndToken(path, "chainsaw")
                || (hasEndToken(path, "axe") && !hasEndToken(path, "pickaxe") && !hasEndToken(path, "wax"))
                || (hasEndToken(path, "hammer") && !path.contains("warhammer"));
    }

    public static boolean looksLikeWeapon(String itemId) {
        String path = pathOf(itemId);
        if (path.isEmpty() || path.equals("bowl") || path.contains("elbow")) {
            return false;
        }
        return hasEndToken(path, "sword")
                || hasEndToken(path, "blade")
                || hasEndToken(path, "dagger")
                || hasEndToken(path, "knife")
                || hasEndToken(path, "katana")
                || hasEndToken(path, "saber")
                || hasEndToken(path, "sabre")
                || hasEndToken(path, "rapier")
                || hasEndToken(path, "claymore")
                || hasEndToken(path, "cutlass")
                || hasEndToken(path, "scimitar")
                || hasEndToken(path, "machete")
                || hasEndToken(path, "trident")
                || hasEndToken(path, "spear")
                || hasEndToken(path, "lance")
                || hasEndToken(path, "javelin")
                || hasEndToken(path, "mace")
                || hasEndToken(path, "warhammer")
                || hasEndToken(path, "scythe")
                || hasEndToken(path, "sickle")
                || hasEndToken(path, "staff")
                || hasEndToken(path, "wand")
                || hasEndToken(path, "scepter")
                || hasEndToken(path, "sceptre")
                || path.equals("bow")
                || hasEndToken(path, "bow")
                || path.contains("bow_")
                || path.equals("crossbow")
                || hasEndToken(path, "crossbow")
                || (hasEndToken(path, "axe") && !hasEndToken(path, "pickaxe") && !hasEndToken(path, "wax"));
    }

    public static boolean looksLikeToolOrWeapon(String itemId) {
        return looksLikeTool(itemId) || looksLikeWeapon(itemId) || looksLikeShield(itemId);
    }

    public static boolean looksLikeShield(String itemId) {
        String path = pathOf(itemId);
        return path.equals("shield")
                || hasEndToken(path, "shield")
                || hasEndToken(path, "buckler");
    }

    public static boolean looksLikeOffhand(String itemId) {
        return looksLikeShield(itemId)
                || pathOf(itemId).contains("totem")
                || pathOf(itemId).contains("lantern")
                || pathOf(itemId).endsWith("torch");
    }

    public static ArmorKind armorKind(String itemId) {
        String path = pathOf(itemId);
        if (path.isEmpty()) {
            return ArmorKind.NONE;
        }
        if (isBodyArmorPath(path)) {
            return ArmorKind.BODY;
        }
        if (hasEndToken(path, "helmet")
                || hasEndToken(path, "helm")
                || hasEndToken(path, "hat")
                || hasEndToken(path, "crown")
                || hasEndToken(path, "circlet")
                || hasEndToken(path, "tiara")
                || hasEndToken(path, "hood")
                || hasEndToken(path, "mask")
                || hasEndToken(path, "visor")
                || hasEndToken(path, "goggles")
                || hasEndToken(path, "headpiece")
                || path.endsWith("_cap")
                || path.equals("cap")) {
            return ArmorKind.HEAD;
        }
        if (hasEndToken(path, "chestplate")
                || hasEndToken(path, "breastplate")
                || hasEndToken(path, "cuirass")
                || hasEndToken(path, "tunic")
                || hasEndToken(path, "robe")
                || hasEndToken(path, "elytra")
                || hasEndToken(path, "harness")
                || path.equals("elytra")
                || hasEndToken(path, "wings")
                || hasEndToken(path, "chest")
                || hasEndToken(path, "shirt")
                || hasEndToken(path, "jacket")
                || hasEndToken(path, "coat")
                || hasEndToken(path, "vest")) {
            return ArmorKind.CHEST;
        }
        if (hasEndToken(path, "leggings")
                || hasEndToken(path, "greaves")
                || hasEndToken(path, "pants")
                || hasEndToken(path, "legs")
                || hasEndToken(path, "chaps")) {
            return ArmorKind.LEGS;
        }
        if (hasEndToken(path, "boots")
                || hasEndToken(path, "boot")
                || hasEndToken(path, "sabatons")
                || hasEndToken(path, "shoes")
                || hasEndToken(path, "sandals")) {
            return ArmorKind.FEET;
        }
        if (hasEndToken(path, "armor") || path.equals("armour") || hasEndToken(path, "armour")) {
            return ArmorKind.ANY;
        }
        return ArmorKind.NONE;
    }

    public static String slotName(String equipmentSlotName) {
        if (equipmentSlotName == null) {
            return "";
        }
        return switch (equipmentSlotName.trim().toUpperCase(Locale.ROOT)) {
            case "HEAD" -> "head";
            case "CHEST" -> "chest";
            case "LEGS" -> "legs";
            case "FEET" -> "feet";
            case "BODY" -> "body";
            default -> equipmentSlotName.trim().toLowerCase(Locale.ROOT);
        };
    }

    private static boolean isHumanoidArmorSlot(String slot) {
        return "head".equals(slot) || "chest".equals(slot) || "legs".equals(slot) || "feet".equals(slot);
    }

    private static boolean isBodyArmorPath(String path) {
        return hasEndToken(path, "wolf_armor")
                || hasEndToken(path, "body_armor")
                || hasEndToken(path, "animal_armor")
                || hasEndToken(path, "canine_armor")
                || hasEndToken(path, "horse_armor")
                || path.equals("wolf_armor");
    }

    /**
     * Path-segment match that avoids prefix collisions ({@code cap} vs {@code capacity}).
     */
    static boolean hasEndToken(String path, String token) {
        if (path == null || token == null || token.isEmpty()) {
            return false;
        }
        return path.equals(token) || path.endsWith("_" + token);
    }
}
