package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;

/** Reserved recipe-serializer registry (sewing removed). */
public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AzsCompanions.MOD_ID);

    private ModRecipeSerializers() {
    }
}
