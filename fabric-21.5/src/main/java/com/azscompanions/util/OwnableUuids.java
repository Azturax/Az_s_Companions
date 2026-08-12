package com.azscompanions.util;

import com.azscompanions.entity.FabricCompanionEntity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Owner UUID helpers for 1.21.5 (getOwnerUUID / setOwnerUUID removed). */
public final class OwnableUuids {
    private OwnableUuids() {
    }

    @Nullable
    public static UUID get(@Nullable Object entity) {
        if (entity instanceof FabricCompanionEntity companion) {
            return companion.getOwnerUuid();
        }
        if (entity instanceof OwnableEntity ownable) {
            EntityReference<LivingEntity> ref = ownable.getOwnerReference();
            return ref != null ? ref.getUUID() : null;
        }
        return null;
    }

    public static void set(TamableAnimal animal, @Nullable UUID owner) {
        if (owner == null) {
            animal.setOwnerReference(null);
        } else {
            animal.setOwnerReference(new EntityReference<>(owner));
        }
    }

    public static void set(TamableAnimal animal, LivingEntity owner) {
        animal.setOwner(owner);
    }

    public static void set(AbstractHorse horse, LivingEntity owner) {
        horse.setOwner(owner);
    }
}
