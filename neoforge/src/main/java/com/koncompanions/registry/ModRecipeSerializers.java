package com.koncompanions.registry;

import com.koncompanions.KonCompanions;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Reserved recipe-serializer registry (sewing removed). */
public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, KonCompanions.MOD_ID);

    private ModRecipeSerializers() {
    }
}
