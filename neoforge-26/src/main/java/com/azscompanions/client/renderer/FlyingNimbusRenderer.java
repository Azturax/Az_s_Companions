package com.azscompanions.client.renderer;

import com.azscompanions.entity.FlightAuraSupport;
import com.azscompanions.entity.FlyingNimbusEntity;
import com.azscompanions.entity.JindujunSupport;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Billboard yellow Flying Nimbus + cloud-height motion trails (no rising particles).
 */
public final class FlyingNimbusRenderer extends EntityRenderer<FlyingNimbusEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("azscompanions", "textures/entity/companion/flying_nimbus.png");
    private static final int FULLBRIGHT = 0x00F000F0;

    public FlyingNimbusRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.6f;
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingNimbusEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(
            FlyingNimbusEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        float size = 0.85f;
        int rgb = FlightAuraSupport.DEFAULT_NIMBUS_RGB;
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        poseStack.pushPose();
        poseStack.translate(0.0d, JindujunSupport.HEIGHT * 0.35d, 0.0d);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        drawQuad(poseStack, buffer, RenderType.eyes(TEXTURE), size * 1.25f, r, g, b, 255);
        drawQuad(poseStack, buffer, RenderType.entityCutoutNoCull(TEXTURE), size, r, g, b, 255);
        poseStack.popPose();

        Vec3 delta = entity.getDeltaMovement();
        boolean moving = FlightAuraSupport.movingFastEnough(delta.x, delta.y, delta.z);
        if (FlightAuraSupport.shouldShowNimbusTrail(entity.isVehicle(), moving)) {
            Vec3 pos = entity.getPosition(partialTicks);
            FlightAuraRenderer.render(
                    entity,
                    partialTicks,
                    poseStack,
                    buffer,
                    FlightAuraSupport.resolveNimbusTrailRgb(null),
                    1.1f,
                    delta.y > 0.05d,
                    false,
                    false,
                    true,
                    pos.x,
                    pos.y,
                    pos.z,
                    entityYaw);
        } else {
            FlightAuraRenderer.clearTrail(entity.getUUID());
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
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
        put(consumer, matrix, pose, -half, -half * 0.55f, 0, 1, r, g, b, alpha);
        put(consumer, matrix, pose, -half, half * 0.55f, 0, 0, r, g, b, alpha);
        put(consumer, matrix, pose, half, half * 0.55f, 1, 0, r, g, b, alpha);
        put(consumer, matrix, pose, half, -half * 0.55f, 1, 1, r, g, b, alpha);
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
