package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AzsCompanions.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<CompanionEntity>> COMPANION =
            ENTITY_TYPES.register("companion", () ->
                    EntityType.Builder.of(CompanionEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.85f)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("companion"));

    private ModEntities() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(COMPANION.get(), CompanionEntity.createAttributes().build());
    }
}
