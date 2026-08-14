package com.azscompanions.event;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionDeathPersistenceSupport;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionInventoryPersistence;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CompanionDeathEvents {
    private CompanionDeathEvents() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof CompanionEntity companion) {
            CompanionDeathPersistenceSupport.persistOnDeath(companion);
        }
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