package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AzsCompanions.MOD_ID);

    public static final RegistryObject<SoundEvent> KON_GREETING = register("kon.greeting");
    public static final RegistryObject<SoundEvent> KON_IDLE = register("kon.idle");
    public static final RegistryObject<SoundEvent> KON_TASK_PROGRESS = register("kon.task_progress");
    public static final RegistryObject<SoundEvent> KON_DANGER = register("kon.danger");
    public static final RegistryObject<SoundEvent> KON_SUCCESS = register("kon.success");
    public static final RegistryObject<SoundEvent> KON_LOW_HEALTH = register("kon.low_health");
    public static final RegistryObject<SoundEvent> KON_HUNGER = register("kon.hunger");
    public static final RegistryObject<SoundEvent> KON_INVENTORY_FULL = register("kon.inventory_full");
    public static final RegistryObject<SoundEvent> KON_RETURN_HOME = register("kon.return_home");

    private static RegistryObject<SoundEvent> register(String path) {
        return SOUND_EVENTS.register(path, () ->
                SoundEvent.createVariableRangeEvent(new ResourceLocation(AzsCompanions.MOD_ID, path)));
    }

    private ModSounds() {
    }
}
