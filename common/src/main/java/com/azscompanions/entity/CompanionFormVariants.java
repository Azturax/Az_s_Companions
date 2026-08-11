package com.azscompanions.entity;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Mob-form visual variants (wolf coats, cat breeds, fox/rabbit types, sheep wool).
 * IDs match Minecraft entity NBT where possible; empty string means “no variants / default”.
 */
public final class CompanionFormVariants {
    public static final String NBT_KEY = "CompanionFormVariant";

    private static final List<String> WOLF = List.of(
            "minecraft:pale",
            "minecraft:woods",
            "minecraft:ashen",
            "minecraft:black",
            "minecraft:chestnut",
            "minecraft:rusty",
            "minecraft:snowy",
            "minecraft:spotted",
            "minecraft:striped"
    );

    private static final List<String> CAT = List.of(
            "minecraft:tabby",
            "minecraft:black",
            "minecraft:red",
            "minecraft:siamese",
            "minecraft:british_shorthair",
            "minecraft:calico",
            "minecraft:persian",
            "minecraft:ragdoll",
            "minecraft:white",
            "minecraft:jellie",
            "minecraft:all_black"
    );

    private static final List<String> FOX = List.of("red", "snow");

    private static final List<String> RABBIT = List.of(
            "brown",
            "white",
            "black",
            "white_splotched",
            "gold",
            "salt",
            "evil"
    );

    private static final List<String> SHEEP = List.of(
            "white",
            "orange",
            "magenta",
            "light_blue",
            "yellow",
            "lime",
            "pink",
            "gray",
            "light_gray",
            "cyan",
            "purple",
            "blue",
            "brown",
            "green",
            "red",
            "black"
    );

    private static final Map<String, List<String>> BY_FORM = Map.of(
            CompanionForm.WOLF.serializedName(), WOLF,
            CompanionForm.CAT.serializedName(), CAT,
            CompanionForm.FOX.serializedName(), FOX,
            CompanionForm.RABBIT.serializedName(), RABBIT,
            CompanionForm.SHEEP.serializedName(), SHEEP
    );

    private CompanionFormVariants() {
    }

    public static boolean hasVariants(CompanionForm form) {
        return form != null && !variantsOf(form).isEmpty();
    }

    public static List<String> variantsOf(CompanionForm form) {
        if (form == null) {
            return List.of();
        }
        List<String> list = BY_FORM.get(form.serializedName());
        return list == null ? List.of() : list;
    }

    public static String defaultVariant(CompanionForm form) {
        List<String> list = variantsOf(form);
        return list.isEmpty() ? "" : list.get(0);
    }

    /**
     * Clamp to a known id for the form, or default. Unknown / blank → default (or "").
     */
    public static String normalize(CompanionForm form, String variant) {
        List<String> list = variantsOf(form);
        if (list.isEmpty()) {
            return "";
        }
        if (variant == null || variant.isBlank()) {
            return list.get(0);
        }
        String key = variant.trim().toLowerCase(Locale.ROOT);
        for (String id : list) {
            if (id.equalsIgnoreCase(key) || stripNs(id).equalsIgnoreCase(stripNs(key))) {
                return id;
            }
        }
        return list.get(0);
    }

    /** Cycle by {@code delta} (−1 / +1). Forms without variants return "". */
    public static String cycle(CompanionForm form, String current, int delta) {
        List<String> list = variantsOf(form);
        if (list.isEmpty()) {
            return "";
        }
        String normalized = normalize(form, current);
        int index = list.indexOf(normalized);
        if (index < 0) {
            index = 0;
        }
        int size = list.size();
        int step = delta % size;
        if (step < 0) {
            step += size;
        }
        return list.get((index + step) % size);
    }

    public static String displayLabel(String variantId) {
        if (variantId == null || variantId.isBlank()) {
            return "";
        }
        String raw = stripNs(variantId).replace('_', ' ');
        if (raw.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    /** RabbitType NBT (0–5, 99 evil). */
    public static int rabbitTypeId(String variantId) {
        return switch (stripNs(variantId == null ? "" : variantId).toLowerCase(Locale.ROOT)) {
            case "white" -> 1;
            case "black" -> 2;
            case "white_splotched" -> 3;
            case "gold" -> 4;
            case "salt" -> 5;
            case "evil" -> 99;
            default -> 0;
        };
    }

    /** Sheep Color NBT (DyeColor id 0–15). */
    public static int sheepColorId(String variantId) {
        List<String> list = SHEEP;
        String key = stripNs(variantId == null ? "" : variantId).toLowerCase(Locale.ROOT);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(key)) {
                return i;
            }
        }
        return 0;
    }

    public static List<String> allFormKeysWithVariants() {
        return Collections.unmodifiableList(List.copyOf(BY_FORM.keySet()));
    }

    private static String stripNs(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }
}
