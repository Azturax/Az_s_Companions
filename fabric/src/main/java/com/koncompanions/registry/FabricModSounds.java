package com.koncompanions.registry;

import com.koncompanions.KonCompanionsFabric;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class FabricModSounds {
    public static SoundEvent KON_GREETING;
    public static SoundEvent KON_SUCCESS;
    public static SoundEvent KON_DANGER;

    private FabricModSounds() {
    }

    public static void register() {
        KON_GREETING = register("kon.greeting");
        KON_SUCCESS = register("kon.success");
        KON_DANGER = register("kon.danger");
    }

    private static SoundEvent register(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(KonCompanionsFabric.MOD_ID, path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
