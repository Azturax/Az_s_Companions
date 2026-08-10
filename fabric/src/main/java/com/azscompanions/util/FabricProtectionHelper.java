package com.azscompanions.util;

import com.azscompanions.compat.FabricClaimProtectionApi;
import com.azscompanions.compat.ftb.FtbCompat;
import com.azscompanions.entity.FabricCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Fabric safety checks for world modification (claim mods + FTB Chunks soft-dep).
 * Presence/pathfinding into claims is always allowed; only interactions are gated.
 */
public final class FabricProtectionHelper {
    private FabricProtectionHelper() {
    }

    public static boolean isProtectedBlock(Level level, BlockPos pos, Player owner) {
        if (!FtbCompat.chunksBlockInteractionEnabled()) {
            return false;
        }
        return FabricClaimProtectionApi.isClaimProtected(level, pos, owner);
    }

    public static boolean canCompanionModify(Level level, BlockPos pos, FabricCompanionEntity companion) {
        if (!FtbCompat.chunksBlockInteractionEnabled()) {
            return true;
        }
        Player owner = companion.getOwner();
        if (FabricClaimProtectionApi.isClaimProtected(level, pos, owner)) {
            return FabricClaimProtectionApi.canModify(level, pos, companion);
        }
        return true;
    }
}
