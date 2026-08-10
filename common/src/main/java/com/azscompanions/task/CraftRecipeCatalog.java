package com.azscompanions.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dynamic catalog of crafting recipes refreshed on server start from the recipe manager.
 * Maps result item id → recipe ids that produce it.
 */
public final class CraftRecipeCatalog {
    private static volatile Map<String, List<String>> BY_RESULT = Map.of();
    private static volatile Set<String> RECIPE_IDS = Set.of();

    private CraftRecipeCatalog() {
    }

    /**
     * @param resultToRecipes map of lowercase item id → recipe id strings
     */
    public static void refresh(Map<String, ? extends Collection<String>> resultToRecipes) {
        Map<String, List<String>> next = new LinkedHashMap<>();
        Set<String> ids = new LinkedHashSet<>();
        if (resultToRecipes != null) {
            for (Map.Entry<String, ? extends Collection<String>> e : resultToRecipes.entrySet()) {
                if (e.getKey() == null || e.getKey().isBlank() || e.getValue() == null) {
                    continue;
                }
                String result = e.getKey().trim().toLowerCase(Locale.ROOT);
                List<String> list = new ArrayList<>();
                for (String recipeId : e.getValue()) {
                    if (recipeId == null || recipeId.isBlank()) {
                        continue;
                    }
                    String rid = recipeId.trim().toLowerCase(Locale.ROOT);
                    list.add(rid);
                    ids.add(rid);
                }
                if (!list.isEmpty()) {
                    next.put(result, Collections.unmodifiableList(list));
                }
            }
        }
        BY_RESULT = Collections.unmodifiableMap(next);
        RECIPE_IDS = Collections.unmodifiableSet(ids);
    }

    public static Optional<String> firstRecipeForResult(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }
        List<String> list = BY_RESULT.get(itemId.trim().toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(list.getFirst());
    }

    public static List<String> recipesForResult(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return List.of();
        }
        List<String> list = BY_RESULT.get(itemId.trim().toLowerCase(Locale.ROOT));
        return list == null ? List.of() : list;
    }

    public static boolean hasRecipe(String recipeId) {
        return recipeId != null && RECIPE_IDS.contains(recipeId.trim().toLowerCase(Locale.ROOT));
    }

    public static int recipeCount() {
        return RECIPE_IDS.size();
    }

    public static int resultCount() {
        return BY_RESULT.size();
    }

    public static Map<String, List<String>> newBuffer() {
        return new LinkedHashMap<>();
    }
}
