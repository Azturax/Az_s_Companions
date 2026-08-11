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
    public static final String NBT_IDLE = "NimbusIdleTicks";

    /** Uniform size multiplier vs original 1× cloud (hitbox + render + particle stream). */
    public static final float SCALE = 2.5f;

    /** Horizontal cruise speed while steered. */
    public static final double FLY_SPEED = 0.55d;
    /** Vertical climb/descend speed (jump / sneak). */
    public static final double VERTICAL_SPEED = 0.42d;
    /** Soft damp when no input. */
    public static final double IDLE_DAMP = 0.86d;

    /** Cloud collision box (1× base × {@link #SCALE}). */
    public static final float WIDTH = 1.35f * SCALE;
    public static final float HEIGHT = 0.55f * SCALE;

    /**
     * Rider seat Y above entity feet — flush on the Blockbench cloud deck.
     * Deck top is ~0.375 blocks at 1× (6px); sit slightly into the fluff.
     * (Do not use {@link #HEIGHT} here — hitbox is taller than the mesh.)
     */
    public static final double RIDER_Y_OFFSET = 0.32d * SCALE;

    /** Unridden / untouched continuous time before the cloud dismisses itself. */
    public static final int IDLE_DESPAWN_SECONDS = 56;
    public static final int IDLE_DESPAWN_TICKS = IDLE_DESPAWN_SECONDS * 20;

    /** Archaeology brush chance in Trail Ruins (taiga only). Was 2%; kept very rare. */
    public static final float TRAIL_RUINS_LOOT_CHANCE = 0.005f;

    private JindujunSupport() {
    }

    /** Advance idle counter while unmounted; reset to 0 while ridden. */
    public static int nextIdleTicks(boolean hasPassenger, int idleTicks) {
        return hasPassenger ? 0 : idleTicks + 1;
    }

    public static boolean shouldDespawnFromIdle(int idleTicks) {
        return idleTicks >= IDLE_DESPAWN_TICKS;
    }

    /**
     * Client-only helper: emit shaped {@code ENCHANT} particles for a ridden nimbus.
     * Loaders pass a spawn lambda that calls {@code level.addParticle(ParticleTypes.ENCHANT, ...)}.
     * Origin is always the cloud entity — never the passenger.
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
