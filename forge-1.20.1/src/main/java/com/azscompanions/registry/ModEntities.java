package com.azscompanions.registry;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AzsCompanions.MOD_ID);

    public static final RegistryObject<EntityType<CompanionEntity>> COMPANION =
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
