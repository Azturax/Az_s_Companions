package com.azscompanions.entity;

/**
 * Shared combat targeting policy so PASSIVE companions (including Kon) fight hostiles
 * like any other combat-capable companion, while staying friendly toward players/pets.
 */
public final class CompanionCombatTargetSupport {
    private CompanionCombatTargetSupport() {
    }

    /**
     * Whether the hostile-target goal should run at all (attitude / team no longer gate this).
     * Combat permission + server allowCombat are checked by loader goals.
     */
    public static boolean wantsCombatTargets() {
        return true;
    }

    /**
     * Prey filter for proactive aggro.
     *
     * @param attitudeHostile companion attitude is HOSTILE
     * @param teamRival       rival team companion
     * @param targetFriendlyCategory vanilla {@code MobCategory#isFriendly()} (animals etc.)
     * @param targetIsPlayer  living player (never random-agro unless hostile attitude / rival)
     */
    public static boolean isValidHostilePrey(
            boolean allowedCombatant,
            boolean protectedEntity,
            boolean teamRival,
            boolean attitudeHostile,
            boolean targetFriendlyCategory,
            boolean targetIsPlayer) {
        if (!allowedCombatant || protectedEntity) {
            return false;
        }
        if (teamRival) {
            return true;
        }
        if (attitudeHostile) {
            // Hostile attitude: any allowed combatant (still not owner/trusted — loader gate).
            return true;
        }
        // PASSIVE / Kon: proactively attack monsters / non-friendly mobs, never random players.
        if (targetIsPlayer || targetFriendlyCategory) {
            return false;
        }
        return true;
    }

    /**
     * Whether a companion may deal damage to an already-acquired target.
     * Aligns Fabric with NeoForge: hostiles are fair game once targeted; neutrals need a hurt link
     * when {@code attackNeutralsOnlyIfHit} is on.
     */
    public static boolean canAttackAcquiredTarget(
            boolean combatAllowed,
            boolean allowedCombatant,
            boolean protectedEntity,
            boolean teamRival,
            boolean attitudeHostile,
            boolean targetFriendlyCategory,
            boolean attackNeutralsOnlyIfHit,
            boolean hurtLinkWithTargetOrOwner) {
        if (!combatAllowed || !allowedCombatant || protectedEntity) {
            return false;
        }
        if (teamRival || attitudeHostile) {
            return true;
        }
        if (!targetFriendlyCategory) {
            // Monster / hostile category — always OK once targeted (owner-defend / aggro goals).
            return true;
        }
        // Neutral / friendly category: only if hit-linked when config says so.
        if (attackNeutralsOnlyIfHit) {
            return hurtLinkWithTargetOrOwner;
        }
        return hurtLinkWithTargetOrOwner;
    }
}
