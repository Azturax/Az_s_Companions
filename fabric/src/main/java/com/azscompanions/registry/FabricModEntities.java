package com.azscompanions.registry;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricFlyingNimbusEntity;
import com.azscompanions.entity.JindujunSupport;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class FabricModEntities {
    public static EntityType<FabricCompanionEntity> COMPANION;
    public static EntityType<FabricFlyingNimbusEntity> FLYING_NIMBUS;

    private FabricModEntities() {
    }

    public static void register() {
        COMPANION = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "companion"),
                EntityType.Builder.of(FabricCompanionEntity::new, MobCategory.CREATURE)
                        .sized(0.6f, 1.85f)
                        .clientTrackingRange(10)
                        .updateInterval(3)
                        .build("companion"));
        FabricDefaultAttributeRegistry.register(COMPANION, FabricCompanionEntity.createAttributes());

        FLYING_NIMBUS = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, JindujunSupport.ENTITY_ID),
                EntityType.Builder.of(FabricFlyingNimbusEntity::new, MobCategory.MISC)
                        .sized(JindujunSupport.WIDTH, JindujunSupport.HEIGHT)
                        .clientTrackingRange(10)
                        .updateInterval(1)
                        .fireImmune()
                        .build(JindujunSupport.ENTITY_ID));
        FabricDefaultAttributeRegistry.register(FLYING_NIMBUS, FabricFlyingNimbusEntity.createAttributes());
    }
}
