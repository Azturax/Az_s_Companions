package com.azscompanions.event;

import com.azscompanions.entity.FabricCompanionEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;

/**
 * Fabric damage/death gates so Draconic Evolution and similar OP weapons cannot bypass
 * vanilla {@code hurt}/{@code isInvulnerableTo}. CCI / temporary summons stay mortal.
 */
public final class FabricCompanionInvincibilityEvents {
    private FabricCompanionInvincibilityEvents() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof FabricCompanionEntity companion && companion.isFullyInvincible()) {
                companion.setHealth(companion.getMaxHealth());
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof FabricCompanionEntity companion && companion.isFullyInvincible()) {
                companion.setHealth(companion.getMaxHealth());
                return false;
            }
            return true;
        });
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (entity instanceof FabricCompanionEntity companion && companion.isFullyInvincible()) {
                companion.setHealth(companion.getMaxHealth());
                return false;
            }
            return true;
        });
    }
}
