package com.azscompanions.client.renderer;

import com.azscompanions.entity.FlightAuraSupport;
import com.azscompanions.entity.FlightAuraTrailBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DBZ-style soft ki shell + foot-level motion afterimages (Sodium/Iris-safe cutout + eyes).
 * Trails never rise into first-person view — samples locked to feet / cloud height.
 */
public final class FlightAuraRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("azscompanions", "textures/entity/companion/flight_aura.png");
    private static final int FULLBRIGHT = 0x00F000F0;
    private static final Map<UUID, FlightAuraTrailBuffer> TRAILS = new ConcurrentHashMap<>();
    private static final double MIN_SAMPLE_DIST_SQ = 0.04d * 0.04d;

    private FlightAuraRenderer() {
    }

    public static void clearTrail(UUID id) {
        if (id != null) {
            TRAILS.remove(id);
        }
    }

    /**
     * @param worldFeetX/Y/Z entity feet (or cloud) world position this frame
     * @param firstPersonLocal skip body shell for local FP camera
     * @param drawShell        soft bubble around lower body
     */
    public static void render(
            Entity entity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int colorRgb,
            float bodyScale,
            boolean ascending,
            boolean firstPersonLocal,
            boolean drawShell,
            boolean recordTrail,
            double worldFeetX,
            double worldFeetY,
            double worldFeetZ,
            float cameraYaw
    ) {
        if (entity == null || !entity.isAlive()) {
            return;
        }
        int r = FlightAuraSupport.red(colorRgb);
        int g = FlightAuraSupport.green(colorRgb);
        int b = FlightAuraSupport.blue(colorRgb);
        float pulse = FlightAuraSupport.pulseScale(entity.tickCount, partialTicks, ascending);

        if (recordTrail) {
            FlightAuraTrailBuffer trail = TRAILS.computeIfAbsent(
                    entity.getUUID(), id -> new FlightAuraTrailBuffer(FlightAuraSupport.TRAIL_LENGTH));
            double ty = worldFeetY + FlightAuraSupport.TRAIL_Y_OFFSET;
            if (trail.shouldAccept(worldFeetX, ty, worldFeetZ, MIN_SAMPLE_DIST_SQ)) {
                trail.push(worldFeetX, ty, worldFeetZ);
            }
            drawTrails(entity, poseStack, buffer, trail, r, g, b, bodyScale, cameraYaw);
        } else {
            clearTrail(entity.getUUID());
        }

        if (drawShell && FlightAuraSupport.shouldDrawBodyShell(firstPersonLocal)) {
            float height = entity instanceof LivingEntity living ? living.getBbHeight() : 1.0f;
            float shellY = FlightAuraSupport.shellOffsetY(height);
            float half = FlightAuraSupport.SHELL_HALF_SIZE * Math.max(0.35f, bodyScale) * pulse;
            poseStack.pushPose();
            poseStack.translate(0.0d, shellY, 0.0d);
            poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
            // Soft outer + cutout core + hot core — low height only.
            drawQuad(poseStack, buffer, RenderType.eyes(TEXTURE), half * 1.55f, r, g, b, 255);
            drawQuad(poseStack, buffer, RenderType.entityCutoutNoCull(TEXTURE), half, r, g, b, 255);
            drawQuad(poseStack, buffer, RenderType.eyes(TEXTURE), half * 0.55f, 255, 240, 200, 255);
            poseStack.popPose();
        }
    }

    private static void drawTrails(
            Entity entity,
            PoseStack poseStack,
            MultiBufferSource buffer,
            FlightAuraTrailBuffer trail,
            int r,
            int g,
            int b,
            float bodyScale,
            float cameraYaw
    ) {
        double[] sample = new double[3];
        // Draw oldest first so newest sits on top.
        for (int i = trail.size() - 1; i >= 1; i--) {
            if (!trail.getFromNewest(i, sample)) {
                continue;
            }
            float half = FlightAuraSupport.trailHalfSize(i, bodyScale);
            int alpha = FlightAuraSupport.trailAlpha(i);
            double dx = sample[0] - entity.getX();
            double dy = sample[1] - entity.getY();
            double dz = sample[2] - entity.getZ();
            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            poseStack.mulPose(Axis.YP.rotationDegrees(-cameraYaw));
            drawQuad(poseStack, buffer, RenderType.eyes(TEXTURE), half * 1.35f, r, g, b, alpha);
            drawQuad(poseStack, buffer, RenderType.entityCutoutNoCull(TEXTURE), half, r, g, b, Math.min(255, alpha + 30));
            poseStack.popPose();
        }
    }

    private static void drawQuad(
            PoseStack poseStack,
            MultiBufferSource buffer,
            RenderType type,
            float half,
            int r,
            int g,
            int b,
            int alpha
    ) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        VertexConsumer consumer = buffer.getBuffer(type);
        put(consumer, matrix, pose, -half, -half, 0, 1, r, g, b, alpha);
        put(consumer, matrix, pose, -half, half, 0, 0, r, g, b, alpha);
        put(consumer, matrix, pose, half, half, 1, 0, r, g, b, alpha);
        put(consumer, matrix, pose, half, -half, 1, 1, r, g, b, alpha);
    }

    private static void put(
            VertexConsumer consumer,
            Matrix4f matrix,
            PoseStack.Pose pose,
            float x,
            float y,
            float u,
            float v,
            int r,
            int g,
            int b,
            int a
    ) {
        consumer.addVertex(matrix, x, y, 0.0f)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULLBRIGHT)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
