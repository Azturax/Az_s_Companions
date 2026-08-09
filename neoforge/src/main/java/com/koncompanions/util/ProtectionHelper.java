package com.koncompanions.util;

import com.koncompanions.compat.ClaimProtectionApi;
import com.koncompanions.config.CommonConfig;
import com.koncompanions.data.ModTags;
import com.koncompanions.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Central safety checks for combat and world modification.
 */
public final class ProtectionHelper {
    private ProtectionHelper() {
    }

    public static boolean isProtectedEntity(LivingEntity entity) {
        return entity.getType().is(ModTags.EntityTypes.PROTECTED_ENTITIES);
    }

    public static boolean isProtectedBlock(Level level, BlockPos pos, @Nullable Player owner) {
        if (level.getBlockState(pos).is(ModTags.Blocks.BLACKLISTED_BLOCKS)) {
            return true;
        }
        if (CommonConfig.RESPECT_CLAIM_MODS.get()) {
            return ClaimProtectionApi.isClaimProtected(level, pos, owner);
        }
        return false;
    }

    public static boolean canCompanionModify(Level level, BlockPos pos, CompanionEntity companion) {
        if (level.getBlockState(pos).is(ModTags.Blocks.BLACKLISTED_BLOCKS)) {
            return false;
        }
        Player owner = companion.getOwner();
        if (CommonConfig.RESPECT_CLAIM_MODS.get() && ClaimProtectionApi.isClaimProtected(level, pos, owner)) {
            return ClaimProtectionApi.canModify(level, pos, companion);
        }
        return true;
    }
}
