package com.azscompanions.event;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionDeathPersistenceSupport;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionInventoryPersistence;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Keep companion/Bit inventory on death and snapshot to charm / parent Bits store.
 */
public final class CompanionDeathEvents {
    private CompanionDeathEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof CompanionEntity companion)) {
            return;
        }
        if (companion.isFullyInvincible()) {
            event.setCanceled(true);
            companion.setHealth(companion.getMaxHealth());
            return;
        }
        CompanionDeathPersistenceSupport.persistOnDeath(companion);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof CompanionEntity)) {
            return;
        }
        if (CompanionInventoryPersistence.shouldKeepInventoryOnDeath(ServerConfig.KEEP_INVENTORY_ON_DEATH.get())) {
            event.setCanceled(true);
            event.getDrops().clear();
        }
    }
}
