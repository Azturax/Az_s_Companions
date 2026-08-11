package com.azscompanions.client.renderer;

import com.azscompanions.client.model.JindujunModel;
import com.azscompanions.entity.FlyingNimbusEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Blockbench Jindujun cloud mesh. Enchant-shaped particle stream is spawned from the entity tick.
 */
public final class FlyingNimbusRenderer extends EntityRenderer<FlyingNimbusEntity> {
    private final JindujunModel model;

    public FlyingNimbusRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.55f;
        this.model = new JindujunModel(context.bakeLayer(JindujunModel.LAYER));
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingNimbusEntity entity) {
        return JindujunModel.TEXTURE;
    }

    @Override
    public void render(
            FlyingNimbusEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        poseStack.pushPose();
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0d, -1.501d, 0.0d);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - entityYaw));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(JindujunModel.TEXTURE));
        model.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
