package com.koncompanions.registry;

import com.koncompanions.KonCompanions;
import com.koncompanions.loot.AddItemLootModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, KonCompanions.MOD_ID);

    static {
        SERIALIZERS.register("add_item", () -> AddItemLootModifier.CODEC);
    }

    private ModLootModifiers() {
    }
}
