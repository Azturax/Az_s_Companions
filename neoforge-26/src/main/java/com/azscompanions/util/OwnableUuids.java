package com.azscompanions.util;

import com.azscompanions.entity.CompanionEntity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Owner UUID helpers for 1.21.5 (getOwnerUUID / setOwnerUUID removed). */
public final class OwnableUuids {
    private OwnableUuids() {
    }

    @Nullable
    public static UUID get(@Nullable Object entity) {
        if (entity instanceof CompanionEntity companion) {
            return companion.getOwnerUuid();
        }
        if (entity instanceof OwnableEntity ownable) {
            EntityReference<LivingEntity> ref = ownable.getOwnerReference();
            return ref != null ? ref.getUUID() : null;
        }
        return null;
    }

}