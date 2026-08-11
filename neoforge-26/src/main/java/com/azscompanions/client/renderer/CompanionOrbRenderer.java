package com.azscompanions.client.renderer;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionOrbSettings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Billboard glowing orb (NeoForge 26.2). Sodium/Oculus-safe cutout + eyes emissive.
 * Wired when the 26.2 submit pipeline gains a custom draw path.
 */
public final class CompanionOrbRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("azscompanions", "textures/entity/companion/glowing_orb.png");
    private static final int FULLBRIGHT = 0x00F000F0;

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
            float floatAmplitude,
            float floatSpeed,
            float bodyScale
    ) {
        float bob = CompanionOrbSettings.bobDeltaY(entity.tickCount, partialTicks, floatAmplitude, floatSpeed);
        float size = 0.55f * Math.max(0.35f, bodyScale);
        int r = CompanionOrbSettings.red(colorRgb);
        int g = CompanionOrbSettings.green(colorRgb);
        int b = CompanionOrbSettings.blue(colorRgb);

        poseStack.pushPose();
        poseStack.translate(0.0d, 0.15d + bob, 0.0d);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        drawQuad(poseStack, buffer, RenderType.eyes(TEXTURE), size * 1.65f, r, g, b, 255, FULLBRIGHT);
        drawQuad(poseStack, buffer, RenderType.entityCutoutNoCull(TEXTURE), size, r, g, b, 255, FULLBRIGHT);
        drawQuad(poseStack, buffer, RenderType.eyes(TEXTURE), size * 0.72f, 255, 255, 255, 255, FULLBRIGHT);
        poseStack.popPose();
    }

    private static void drawQuad(
            PoseStack poseStack,
            MultiBufferSource buffer,
            RenderType type,
            float half,
            int r,
            int g,
            int b,
            int alpha,
            int light
    ) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        VertexConsumer consumer = buffer.getBuffer(type);
        put(consumer, matrix, pose, -half, -half, 0, 1, r, g, b, alpha, light);
        put(consumer, matrix, pose, -half, half, 0, 0, r, g, b, alpha, light);
        put(consumer, matrix, pose, half, half, 1, 0, r, g, b, alpha, light);
        put(consumer, matrix, pose, half, -half, 1, 1, r, g, b, alpha, light);
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
            int a,
            int light
    ) {
        consumer.addVertex(matrix, x, y, 0.0f)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0f, 1.0f, 0.0f);
    }
}
