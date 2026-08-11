package com.azscompanions.entity;

/**
 * Pure helpers for form scare (cat→creeper, wolf→skeleton) and wander-mode casual mob play.
 * Loader AI goals / entity-join hooks apply the Minecraft side.
 */
public final class CompanionMobBehaviorSupport {
    /** Vanilla cat scare range (blocks); wolf→skeleton uses the same. */
    public static final float CREEPER_SCARE_DISTANCE = 6.0f;
    public static final double CREEPER_WALK_SPEED = 1.0d;
    public static final double CREEPER_SPRINT_SPEED = 1.2d;

    public static final float SKELETON_SCARE_DISTANCE = CREEPER_SCARE_DISTANCE;
    public static final double SKELETON_WALK_SPEED = CREEPER_WALK_SPEED;
    public static final double SKELETON_SPRINT_SPEED = CREEPER_SPRINT_SPEED;

    /** Scan radius for casual wander interactions. */
    public static final double INTERACT_RANGE = 7.0d;
    public static final double INTERACT_RANGE_SQR = INTERACT_RANGE * INTERACT_RANGE;
    /** ~1/120 per tick while eligible — rare, not constant. */
    public static final int INTERACT_START_CHANCE = 120;
    public static final int INTERACT_COOLDOWN_MIN = 100;
    public static final int INTERACT_COOLDOWN_MAX = 240;
    public static final int INTERACT_DURATION_MIN = 40;
    public static final int INTERACT_DURATION_MAX = 90;
    public static final double CIRCLE_RADIUS = 2.25d;
    public static final double APPROACH_SPEED = 0.75d;
    public static final double PUSH_STRENGTH = 0.42d;
    public static final double PUNCH_KNOCKBACK = 0.55d;
    /** Playful tap — knockback-focused; optional tiny damage when combat is allowed. */
    public static final float PUNCH_DAMAGE = 0.5f;

    public enum WanderInteractKind {
        CIRCLE,
        PUSH,
        PUNCH,
        SNEAK
    }

    private CompanionMobBehaviorSupport() {
    }

    /** Only cat-form companions scare creepers (vanilla cats / ocelots, not players). */
    public static boolean formScaresCreepers(CompanionForm form) {
        return form == CompanionForm.CAT;
    }

    public static boolean formScaresCreepers(String formSerializedName) {
        if (formSerializedName == null || formSerializedName.isBlank()) {
            return false;
        }
        return formScaresCreepers(CompanionForm.byName(formSerializedName));
    }

    /** Only wolf/dog-form companions scare skeletons (and skeleton variants). */
    public static boolean formScaresSkeletons(CompanionForm form) {
        return form == CompanionForm.WOLF;
    }

    public static boolean formScaresSkeletons(String formSerializedName) {
        if (formSerializedName == null || formSerializedName.isBlank()) {
            return false;
        }
        return formScaresSkeletons(CompanionForm.byName(formSerializedName));
    }

    /**
     * Wander-mode play only — never Follow/Stay/Sit, never while fighting or sleeping.
     */
    public static boolean canStartWanderInteract(
            boolean wanderMode,
            boolean sitting,
            boolean sleeping,
            boolean hasCombatTarget,
            int cooldownTicks) {
        if (!wanderMode || sitting || sleeping || hasCombatTarget) {
            return false;
        }
        return cooldownTicks <= 0;
    }

    public static boolean rollStart(int randomBoundExclusive) {
        if (randomBoundExclusive <= 0) {
            return false;
        }
        return randomBoundExclusive % INTERACT_START_CHANCE == 0;
    }

    /**
     * Soft target filter (pure). Loaders also reject claim-protected / owner pets.
     */
    public static boolean isValidInteractTarget(
            boolean alive,
            boolean isPlayer,
            boolean isCompanion,
            boolean isOwnerOrTrustedPet,
            boolean isBossLike) {
        if (!alive || isPlayer || isCompanion || isOwnerOrTrustedPet || isBossLike) {
            return false;
        }
        return true;
    }

    public static boolean isBossLikeEntityId(String entityTypeId) {
        if (entityTypeId == null || entityTypeId.isBlank()) {
            return false;
        }
        String id = entityTypeId.toLowerCase();
        return id.endsWith(":ender_dragon")
                || id.endsWith(":wither")
                || id.endsWith(":warden")
                || id.endsWith(":elder_guardian");
    }

    /**
     * Weighted playful mix: circle often, sneak sometimes, push/punch rarer.
     *
     * @param roll inclusive 0–99
     */
    public static WanderInteractKind pickKind(int roll) {
        int r = Math.floorMod(roll, 100);
        if (r < 40) {
            return WanderInteractKind.CIRCLE;
        }
        if (r < 65) {
            return WanderInteractKind.SNEAK;
        }
        if (r < 85) {
            return WanderInteractKind.PUSH;
        }
        return WanderInteractKind.PUNCH;
    }

    public static int cooldownTicks(int randomInclusiveRange) {
        int span = INTERACT_COOLDOWN_MAX - INTERACT_COOLDOWN_MIN;
        int offset = Math.floorMod(randomInclusiveRange, span + 1);
        return INTERACT_COOLDOWN_MIN + offset;
    }

    public static int durationTicks(int randomInclusiveRange) {
        int span = INTERACT_DURATION_MAX - INTERACT_DURATION_MIN;
        int offset = Math.floorMod(randomInclusiveRange, span + 1);
        return INTERACT_DURATION_MIN + offset;
    }

    /**
     * Point on a ring around the mob for walk-around / sneak approaches.
     *
     * @return {@code [x, z]}
     */
    public static double[] circlePoint(double mobX, double mobZ, double angleRad, double radius) {
        return new double[]{
                mobX + Math.cos(angleRad) * radius,
                mobZ + Math.sin(angleRad) * radius
        };
    }

    /** Horizontal knockback direction from companion toward away-from-self on target. */
    public static double[] knockbackDir(double fromX, double fromZ, double toX, double toZ) {
        double dx = toX - fromX;
        double dz = toZ - fromZ;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-4d) {
            return new double[]{1.0d, 0.0d};
        }
        return new double[]{dx / len, dz / len};
    }

    /**
     * Punch may deal tiny damage only when combat is allowed; otherwise knockback-only.
     */
    public static boolean punchDealsDamage(boolean combatAllowed) {
        return combatAllowed;
    }
}
