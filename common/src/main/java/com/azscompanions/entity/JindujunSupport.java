package com.azscompanions.entity;

/**
 * Flying Nimbus / Jindujun (Überschallwolke) shared constants.
 * Rideable player-controlled cloud — not an AI pet while mounted.
 * Visuals: Blockbench cloud mesh + {@link JindujunParticleSupport} enchant stream (no ki aura).
 */
public final class JindujunSupport {
    public static final String ENTITY_ID = "flying_nimbus";
    public static final String ITEM_ID = "jindujun_whistle";
    public static final String NBT_OWNER = "NimbusOwner";

    /** Horizontal cruise speed while steered. */
    public static final double FLY_SPEED = 0.55d;
    /** Vertical climb/descend speed (jump / sneak). */
    public static final double VERTICAL_SPEED = 0.42d;
    /** Soft damp when no input. */
    public static final double IDLE_DAMP = 0.86d;

    /** Cloud collision box. */
    public static final float WIDTH = 1.35f;
    public static final float HEIGHT = 0.55f;

    /** Rider sits atop the fluff. */
    public static final double RIDER_Y_OFFSET = 0.48d;

    /** Archaeology brush chance in Trail Ruins (taiga only). Was 2%; kept very rare. */
    public static final float TRAIL_RUINS_LOOT_CHANCE = 0.005f;

    private JindujunSupport() {
    }

    /**
     * Client-only helper: emit shaped {@code ENCHANT} particles for a ridden nimbus.
     * Loaders pass a spawn lambda that calls {@code level.addParticle(ParticleTypes.ENCHANT, ...)}.
     */
    @FunctionalInterface
    public interface EnchantParticleSpawner {
        void spawn(double x, double y, double z, double vx, double vy, double vz);
    }

    public static void spawnEnchantStream(
            boolean hasPassenger,
            double dx,
            double dy,
            double dz,
            int tickAge,
            double entityX,
            double entityY,
            double entityZ,
            float yawDegrees,
            EnchantParticleSpawner spawner) {
        if (!JindujunParticleSupport.shouldSpawn(hasPassenger)) {
            return;
        }
        boolean moving = JindujunParticleSupport.movingFastEnough(dx, dy, dz);
        int count = JindujunParticleSupport.particlesThisTick(moving, tickAge);
        float[] offset = new float[3];
        float[] vel = new float[3];
        for (int slot = 0; slot < count; slot++) {
            int idx = JindujunParticleSupport.pointIndex(tickAge, slot);
            JindujunParticleSupport.worldOffset(
                    JindujunParticleSupport.localX(idx),
                    JindujunParticleSupport.localY(idx),
                    JindujunParticleSupport.localZ(idx),
                    yawDegrees,
                    offset);
            JindujunParticleSupport.enchantVelocity(offset[0], offset[1], offset[2], vel);
            spawner.spawn(
                    entityX + offset[0],
                    entityY + offset[1],
                    entityZ + offset[2],
                    vel[0],
                    vel[1],
                    vel[2]);
        }
    }
}
