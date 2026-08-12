package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;

/** Reserved recipe-type registry (sewing removed). */
public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, AzsCompanions.MOD_ID);

    private ModRecipeTypes() {
    }
}
