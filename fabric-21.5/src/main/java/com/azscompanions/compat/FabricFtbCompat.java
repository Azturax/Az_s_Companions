package com.azscompanions.compat;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.compat.ftb.FtbCompat;
import com.azscompanions.compat.ftb.FtbReflectionBridge;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

/**
 * Optional FTB Teams / Chunks / Ranks soft-dep for Fabric. Reflection only.
 */
public final class FabricFtbCompat {
    private FabricFtbCompat() {
    }

    public static void bootstrap() {
        boolean teams = FabricLoader.getInstance().isModLoaded("ftbteams");
        boolean chunks = FabricLoader.getInstance().isModLoaded("ftbchunks");
        boolean ranks = FabricLoader.getInstance().isModLoaded("ftbranks");
        if (!teams && !chunks && !ranks) {
            return;
        }
        FtbCompat.install(new FtbReflectionBridge(teams, chunks, ranks),
                () -> CompanionAiRuntime.get().settings());
        if (chunks) {
            FabricClaimProtectionApi.registerClaimChecker((level, pos, player) -> {
                if (!(player instanceof ServerPlayer sp) || !FtbCompat.chunksBlockInteractionEnabled()) {
                    return false;
                }
                return FtbCompat.shouldPreventBlockEdit(level, pos, sp);
            });
            FabricClaimProtectionApi.registerModifyChecker((level, pos, companion) -> {
                if (!FtbCompat.chunksBlockInteractionEnabled()) {
                    return true;
                }
                if (!(companion.getOwner() instanceof ServerPlayer sp)) {
                    return true;
                }
                return !FtbCompat.shouldPreventBlockEdit(level, pos, sp);
            });
        }
        AzsCompanionsFabric.LOGGER.info(
                "FTB soft-compat active (teams={}, chunks={}, ranks={})", teams, chunks, ranks);
    }
}
