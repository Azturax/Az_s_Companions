package com.azscompanions.entity;

import com.azscompanions.perk.WigglyDogPerkSupport;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 * Shared spawn/dedup policy for owned companions and perk pets (Wiggly, etc.).
 * <p>
 * Login races: the original entity is often still unloading/loading when a tick/charm
 * path summons a second copy next to the player. Wait a short grace, reuse the same UUID
 * if it is already in the world, and cull extras down to the configured cap.
 */
public final class CompanionSpawnGuardSupport {
    /**
     * Player {@code tickCount} window after join during which missing pets must not be
     * re-summoned (chunk/entity lists are still catching up).
     */
    public static final int LOGIN_GRACE_TICKS = 80;

    private CompanionSpawnGuardSupport() {
    }

    public static boolean inLoginGrace(int playerTickCount) {
        return playerTickCount >= 0 && playerTickCount < LOGIN_GRACE_TICKS;
    }

    /** Do not create another copy when this UUID is already living. */
    public static boolean shouldReuseExisting(boolean existingAlive) {
        return existingAlive;
    }

    /**
     * When over the primary-companion cap, keep charm-bound first, then closest.
     * Lower score wins.
     */
    public static <T> T pickPrimaryToKeep(
            List<T> candidates,
            UUID charmBoundUuid,
            Function<T, UUID> uuidOf,
            ToDoubleFunction<T> distanceSq) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (charmBoundUuid != null) {
            for (T candidate : candidates) {
                if (charmBoundUuid.equals(uuidOf.apply(candidate))) {
                    return candidate;
                }
            }
        }
        return WigglyDogPerkSupport.pickOneToKeep(candidates, distanceSq);
    }
}
