package com.koncompanions.registry;

import com.koncompanions.KonCompanions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Reserved recipe-type registry (sewing removed). */
public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, KonCompanions.MOD_ID);

    private ModRecipeTypes() {
    }
}
