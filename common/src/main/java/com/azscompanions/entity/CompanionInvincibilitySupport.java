package com.azscompanions.entity;

/**
 * Kon and Bits must never die from world/combat damage. Dismiss/despawn via charm,
 * logout park, and admin discard still work — those do not go through {@code hurt}/{@code die}.
 */
public final class CompanionInvincibilitySupport {
    private CompanionInvincibilitySupport() {
    }

    /**
     * True for the Kon character (named or definition {@code kon}) and child Bits
     * (leader-linked or named Bit/Bits).
     * <p>
     * CCI / streamer temporary summons are never invincible — they must be able to
     * expire and die after their timed window.
     */
    public static boolean isFullyInvincible(
            boolean konNamed,
            boolean childCompanion,
            String displayName,
            String definitionId) {
        return isFullyInvincible(konNamed, childCompanion, displayName, definitionId, false);
    }

    public static boolean isFullyInvincible(
            boolean konNamed,
            boolean childCompanion,
            String displayName,
            String definitionId,
            boolean cciSummoned) {
        if (cciSummoned) {
            return false;
        }
        if (childCompanion || konNamed) {
            return true;
        }
        if (isBitName(displayName) || isKonName(displayName)) {
            return true;
        }
        return isKonDefinition(definitionId);
    }

    public static boolean isKonName(String displayName) {
        return displayName != null && "Kon".equalsIgnoreCase(displayName.trim());
    }

    public static boolean isBitName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return false;
        }
        String n = displayName.trim();
        return "Bit".equalsIgnoreCase(n) || "Bits".equalsIgnoreCase(n);
    }

    public static boolean isKonDefinition(String definitionId) {
        if (definitionId == null || definitionId.isBlank()) {
            return false;
        }
        String id = definitionId.trim();
        int colon = id.indexOf(':');
        String path = colon >= 0 ? id.substring(colon + 1) : id;
        return "kon".equalsIgnoreCase(path);
    }

    /**
     * Invincible charm companions cancel <em>all</em> incoming damage, including unknown and
     * custom/modded types (Draconic Evolution chaos/wyvern, armor-pierce, absolute, etc.).
     * {@code damageTypeId} is accepted so callers/tests can pass the real id; it is never used
     * as an allowlist. CCI / temporary summons pass {@code fullyInvincible=false}.
     */
    public static boolean shouldCancelDamage(boolean fullyInvincible, String damageTypeId) {
        return fullyInvincible;
    }

    /**
     * True when a mod (or {@code actuallyHurt}) tries to lower health on an invincible companion.
     * Direct {@code setHealth} / health-sync writes after {@code hurt()} returned are the usual
     * Draconic Evolution bypass.
     */
    public static boolean shouldRejectHealthDrop(boolean fullyInvincible, float currentHealth, float proposedHealth) {
        if (!fullyInvincible) {
            return false;
        }
        if (Float.isNaN(proposedHealth)) {
            return true;
        }
        return proposedHealth < currentHealth;
    }

    /** Tick / post-event clamp: invincible companions are restored to max when below it. */
    public static float restoreHealth(boolean fullyInvincible, float health, float maxHealth) {
        if (!fullyInvincible) {
            return health;
        }
        if (Float.isNaN(health) || health < maxHealth) {
            return maxHealth;
        }
        return health;
    }
}
