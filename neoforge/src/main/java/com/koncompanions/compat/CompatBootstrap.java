package com.koncompanions.compat;

import com.koncompanions.KonCompanions;
import com.koncompanions.api.CompanionApi;
import com.koncompanions.compat.optional.FarmingCompatModule;
import com.koncompanions.compat.optional.StorageCompatModule;
import com.koncompanions.compat.optional.TechCompatModule;
import com.koncompanions.compat.optional.VoiceChatCompatModule;
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
        maybeLoad("voicechat", VoiceChatCompatModule::bootstrap);

        // Always register vanilla furnace fuel helper as a machine handler example.
        CompanionApi.registerMachineHandler(new VanillaFurnaceMachineHandler());
        KonCompanions.LOGGER.info("Compatibility bootstrap finished");
    }

    private static void maybeLoad(String modId, Runnable loader) {
        if (ModList.get().isLoaded(modId)) {
            try {
                loader.run();
                KonCompanions.LOGGER.info("Loaded optional compat for {}", modId);
            } catch (Exception e) {
                KonCompanions.LOGGER.warn("Failed optional compat for {}", modId, e);
            }
        }
    }
}
