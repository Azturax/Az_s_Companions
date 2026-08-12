package com.azscompanions.client.renderer;

import com.azscompanions.client.model.KonEarsModel;
import com.azscompanions.perk.SpecialPlayerPerks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Client-only Kon ears on the special UUID player's head.
 * Visible to anyone with the mod installed (UUID check at render time).
 */
public final class KonEarsLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final KonEarsModel ears;

    public KonEarsLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
            EntityModelSet models) {
        super(parent);
        this.ears = new KonEarsModel(models.bakeLayer(KonEarsModel.LAYER));
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        if (!SpecialPlayerPerks.hasKonEars(player.getUUID()) || player.isInvisible()) {
            return;
        }
        poseStack.pushPose();
        getParentModel().getHead().translateAndRotate(poseStack);
        poseStack.translate(0.0d, -0.02d, 0.0d);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(KonEarsModel.TEXTURE));
        ears.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
