package com.azscompanions.compat;

import com.azscompanions.AzsCompanions;
import com.azscompanions.api.CompanionApi;
import com.azscompanions.compat.optional.FarmingCompatModule;
import com.azscompanions.compat.optional.FtbCompatModule;
import com.azscompanions.compat.optional.StorageCompatModule;
import com.azscompanions.compat.optional.TechCompatModule;
import com.azscompanions.compat.optional.VoiceChatCompatModule;
import net.neoforged.fml.ModList;

/**
 * Optional integrations — never hard-depend. Modules activate only if target mods are present
 * or when their internal feature flag is enabled.
 */
public final class CompatBootstrap {
    private CompatBootstrap() {
    }

    public static void bootstrap() {
        ContainerAccessApi.bootstrap();
        ClaimProtectionApi.bootstrap();
        WorkstationHelper.bootstrap();

        maybeLoad("storagedrawers", StorageCompatModule::bootstrap);
        maybeLoad("sophisticatedbackpacks", StorageCompatModule::bootstrap);
        maybeLoad("mekanism", TechCompatModule::bootstrap);
        maybeLoad("create", TechCompatModule::bootstrap);
        maybeLoad("farmersdelight", FarmingCompatModule::bootstrap);
        // Always probe: Simple Voice Chat (`voicechat`) and optional `voicemod` awareness.
        VoiceChatCompatModule.bootstrap();
        FtbCompatModule.bootstrap();

        // Always register vanilla furnace fuel helper as a machine handler example.
        CompanionApi.registerMachineHandler(new VanillaFurnaceMachineHandler());
        AzsCompanions.LOGGER.info("Compatibility bootstrap finished");
    }

    private static void maybeLoad(String modId, Runnable loader) {
        if (ModList.get().isLoaded(modId)) {
            try {
                loader.run();
                AzsCompanions.LOGGER.info("Loaded optional compat for {}", modId);
            } catch (Exception e) {
                AzsCompanions.LOGGER.warn("Failed optional compat for {}", modId, e);
            }
        }
    }
}
