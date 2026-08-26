package com.azscompanions.event;

import com.azscompanions.entity.CompanionPlayerDataSupport;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Flush per-player companion settings/inventory to overworld SavedData on world save.
 */
public final class CompanionPlayerDataEvents {
    private CompanionPlayerDataEvents() {
    }

    @SubscribeEvent
    public static void onLevelSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.getServer() == null || level != level.getServer().overworld()) {
            return;
        }
        CompanionPlayerDataSupport.saveAll(level.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        CompanionPlayerDataSupport.saveAll(event.getServer());
    }
}
