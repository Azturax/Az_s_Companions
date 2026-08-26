package com.azscompanions.event;

import com.azscompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Cancel every damage/death stage at HIGHEST and LOWEST so Draconic Evolution and similar
 * OP weapons cannot apply chaos/bypass damage after vanilla {@code hurt}/{@code isInvulnerableTo}.
 * CCI / temporary summons are not {@link CompanionEntity#isFullyInvincible() fully invincible}.
 */
public final class CompanionInvincibilityEvents {
    private CompanionInvincibilityEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingHighest(LivingIncomingDamageEvent event) {
        cancelIncoming(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncomingLowest(LivingIncomingDamageEvent event) {
        cancelIncoming(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamagePreHighest(LivingDamageEvent.Pre event) {
        cancelDamagePre(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamagePreLowest(LivingDamageEvent.Pre event) {
        cancelDamagePre(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamagePostHighest(LivingDamageEvent.Post event) {
        restore(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamagePostLowest(LivingDamageEvent.Post event) {
        restore(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeathHighest(LivingDeathEvent event) {
        cancelDeath(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeathLowest(LivingDeathEvent event) {
        cancelDeath(event);
    }

    private static void cancelIncoming(LivingIncomingDamageEvent event) {
        if (protect(event.getEntity())) {
            event.setCanceled(true);
            event.setAmount(0.0f);
        }
    }

    private static void cancelDamagePre(LivingDamageEvent.Pre event) {
        if (protect(event.getEntity())) {
            event.setNewDamage(0.0f);
        }
    }

    private static void cancelDeath(LivingDeathEvent event) {
        if (protect(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void restore(LivingEntity entity) {
        protect(entity);
    }

    private static boolean protect(LivingEntity entity) {
        if (entity instanceof CompanionEntity companion && companion.isFullyInvincible()) {
            companion.setHealth(companion.getMaxHealth());
            return true;
        }
        return false;
    }
}
