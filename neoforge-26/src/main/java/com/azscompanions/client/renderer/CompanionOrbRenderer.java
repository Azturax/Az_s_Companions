package com.azscompanions.client.renderer;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionOrbSettings;
import com.azscompanions.entity.CompanionOrbSupport;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Particles-only Glowing Orb (NeoForge 26.2). Soft spherical dust shell — no billboard texture.
 */
public final class CompanionOrbRenderer {
    private static final Map<Integer, Integer> LAST_PARTICLE_TICK = new ConcurrentHashMap<>();
    private static final float[] TMP = new float[3];

    private CompanionOrbRenderer() {
    }

    public static void render(
            CompanionEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int colorRgb,
            int brightness,
            float floatAmplitude,
            float floatSpeed,
            float bodyScale
    ) {
        Level level = entity.level();
        if (!level.isClientSide) {
            return;
        }
        int id = entity.getId();
        int tick = entity.tickCount;
        Integer prev = LAST_PARTICLE_TICK.put(id, tick);
        if (prev != null && prev == tick) {
            return;
        }
        if ((tick & 127) == 0 && LAST_PARTICLE_TICK.size() > 64) {
            LAST_PARTICLE_TICK.entrySet().removeIf(e -> tick - e.getValue() > 40);
        }

        float bob = CompanionOrbSettings.bobDeltaY(tick, partialTicks, floatAmplitude, floatSpeed);
        double cx = entity.getX();
        double cy = entity.getY() + entity.getBbHeight() * 0.5d + 0.15d + bob;
        double cz = entity.getZ();
        int rgb = CompanionOrbSettings.clampRgb(colorRgb);
        float rf = CompanionOrbSettings.red(rgb) / 255.0f;
        float gf = CompanionOrbSettings.green(rgb) / 255.0f;
        float bf = CompanionOrbSettings.blue(rgb) / 255.0f;
        float dustSize = 0.55f + 0.55f * (CompanionOrbSettings.clampBrightness(brightness) / 15.0f);
        float radius = CompanionOrbSupport.particleShellRadius(bodyScale);
        int dust = CompanionOrbSupport.dustParticlesPerTick(brightness, bodyScale);
        DustParticleOptions dustOpts = new DustParticleOptions(new Vector3f(rf, gf, bf), dustSize);
        for (int i = 0; i < dust; i++) {
            CompanionOrbSupport.sampleBallOffset(tick, id * 31 + i, radius, TMP);
            level.addParticle(dustOpts, cx + TMP[0], cy + TMP[1], cz + TMP[2], 0.0d, 0.0d, 0.0d);
        }
        int glow = CompanionOrbSupport.glowParticlesPerTick(brightness);
        for (int i = 0; i < glow; i++) {
            CompanionOrbSupport.sampleBallOffset(tick + 17, id * 17 + i, radius * 0.35f, TMP);
            level.addParticle(
                    ParticleTypes.GLOW,
                    cx + TMP[0], cy + TMP[1], cz + TMP[2],
                    0.0d, 0.0d, 0.0d);
            if (i == 0 && brightness >= CompanionOrbSupport.TORCH_BRIGHTNESS) {
                level.addParticle(ParticleTypes.END_ROD, cx, cy, cz, 0.0d, 0.0d, 0.0d);
            }
        }
    }
}
