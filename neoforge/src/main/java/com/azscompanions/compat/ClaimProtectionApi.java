package com.azscompanions.compat;

import com.azscompanions.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Soft bridge for claim/protection mods. Integrations register predicates at runtime.
 */
public final class ClaimProtectionApi {
    @FunctionalInterface
    public interface ClaimChecker {
        boolean isProtected(Level level, BlockPos pos, @Nullable Player player);
    }

    @FunctionalInterface
    public interface ModifyChecker {
        boolean canModify(Level level, BlockPos pos, CompanionEntity companion);
    }

    private static final List<ClaimChecker> CLAIM_CHECKERS = new ArrayList<>();
    private static final List<ModifyChecker> MODIFY_CHECKERS = new ArrayList<>();

    private ClaimProtectionApi() {
    }

    public static void bootstrap() {
        // Vanilla fallback: no claims.
    }

    public static void registerClaimChecker(ClaimChecker checker) {
        CLAIM_CHECKERS.add(checker);
    }

    public static void registerModifyChecker(ModifyChecker checker) {
        MODIFY_CHECKERS.add(checker);
    }

    public static boolean isClaimProtected(Level level, BlockPos pos, @Nullable Player player) {
        for (ClaimChecker checker : CLAIM_CHECKERS) {
            if (checker.isProtected(level, pos, player)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canModify(Level level, BlockPos pos, CompanionEntity companion) {
        if (MODIFY_CHECKERS.isEmpty()) {
            return !isClaimProtected(level, pos, companion.getOwner());
        }
        for (ModifyChecker checker : MODIFY_CHECKERS) {
            if (!checker.canModify(level, pos, companion)) {
                return false;
            }
        }
        return true;
    }
}
