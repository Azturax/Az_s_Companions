package com.azscompanions.compat.optional;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.compat.ClaimProtectionApi;
import com.azscompanions.compat.ftb.FtbCompat;
import com.azscompanions.compat.ftb.FtbReflectionBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Optional FTB Teams / Chunks / Ranks soft-dep. Reflection only — no compile dependency.
 */
public final class FtbCompatModule {
    private FtbCompatModule() {
    }

    public static void bootstrap() {
        boolean teams = ModList.get().isLoaded("ftbteams");
        boolean chunks = ModList.get().isLoaded("ftbchunks");
        boolean ranks = ModList.get().isLoaded("ftbranks");
        if (!teams && !chunks && !ranks) {
            return;
        }
        FtbCompat.install(new FtbReflectionBridge(teams, chunks, ranks),
                () -> CompanionAiRuntime.get().settings());
        if (chunks) {
            ClaimProtectionApi.registerClaimChecker((level, pos, player) -> {
                if (!(player instanceof ServerPlayer sp) || !FtbCompat.chunksBlockInteractionEnabled()) {
                    return false;
                }
                return FtbCompat.shouldPreventBlockEdit(level, pos, sp);
            });
            ClaimProtectionApi.registerModifyChecker((level, pos, companion) -> {
                if (!FtbCompat.chunksBlockInteractionEnabled()) {
                    return true;
                }
                if (!(companion.getOwner() instanceof ServerPlayer sp)) {
                    return true;
                }
                return !FtbCompat.shouldPreventBlockEdit(level, pos, sp);
            });
        }
        AzsCompanions.LOGGER.info(
                "FTB soft-compat active (teams={}, chunks={}, ranks={})", teams, chunks, ranks);
    }
}
