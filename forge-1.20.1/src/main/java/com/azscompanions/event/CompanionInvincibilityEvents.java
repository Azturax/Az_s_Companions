package com.azscompanions.event;

import com.azscompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Cancel every damage/death stage at HIGHEST and LOWEST so Draconic Evolution and similar
 * OP weapons cannot apply chaos/bypass damage after vanilla {@code hurt}/{@code isInvulnerableTo}.
 * CCI / temporary summons are not {@link CompanionEntity#isFullyInvincible() fully invincible}.
 */
public final class CompanionInvincibilityEvents {
    private CompanionInvincibilityEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackHighest(LivingAttackEvent event) {
        cancelAttack(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackLowest(LivingAttackEvent event) {
        cancelAttack(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurtHighest(LivingHurtEvent event) {
        cancelHurt(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurtLowest(LivingHurtEvent event) {
        cancelHurt(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamageHighest(LivingDamageEvent event) {
        cancelDamage(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamageLowest(LivingDamageEvent event) {
        cancelDamage(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeathHighest(LivingDeathEvent event) {
        cancelDeath(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeathLowest(LivingDeathEvent event) {
        cancelDeath(event);
    }

    private static void cancelAttack(LivingAttackEvent event) {
        if (protect(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void cancelHurt(LivingHurtEvent event) {
        if (protect(event.getEntity())) {
            event.setCanceled(true);
            event.setAmount(0.0f);
        }
    }

    private static void cancelDamage(LivingDamageEvent event) {
        if (protect(event.getEntity())) {
            event.setCanceled(true);
            event.setAmount(0.0f);
        }
    }

    private static void cancelDeath(LivingDeathEvent event) {
        if (protect(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean protect(LivingEntity entity) {
        if (entity instanceof CompanionEntity companion && companion.isFullyInvincible()) {
            companion.setHealth(companion.getMaxHealth());
            return true;
        }
        return false;
    }
}
