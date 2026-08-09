package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, AzsCompanions.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> KON_GREETING = register("kon.greeting");
    public static final DeferredHolder<SoundEvent, SoundEvent> KON_IDLE = register("kon.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> KON_TASK_PROGRESS = register("kon.task_progress");
    public static final DeferredHolder<SoundEvent, SoundEvent> KON_DANGER = register("kon.danger");
    public static final DeferredHolder<SoundEvent, SoundEvent> KON_SUCCESS = register("kon.success");
    public static final DeferredHolder<SoundEvent, SoundEvent> KON_LOW_HEALTH = register("kon.low_health");
    public static final DeferredHolder<SoundEvent, SoundEvent> KON_HUNGER = register("kon.hunger");
    public static final DeferredHolder<SoundEvent, SoundEvent> KON_INVENTORY_FULL = register("kon.inventory_full");
    public static final DeferredHolder<SoundEvent, SoundEvent> KON_RETURN_HOME = register("kon.return_home");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        return SOUND_EVENTS.register(path, () ->
                SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, path)));
    }

    private ModSounds() {
    }
}
