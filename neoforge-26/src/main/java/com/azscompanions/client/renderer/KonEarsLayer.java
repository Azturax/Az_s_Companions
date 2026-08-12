package com.azscompanions.client.renderer;

import com.azscompanions.client.model.KonEarsModel;
import com.azscompanions.perk.SpecialPlayerPerks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.player.PlayerModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;

/** Submits the optional Kon ears mesh on matching player avatars. */
public final class KonEarsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final KonEarsModel ears;

    public KonEarsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models) {
        super(parent);
        this.ears = new KonEarsModel(models.bakeLayer(KonEarsModel.LAYER));
    }

    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            AvatarRenderState state,
            float yRot,
            float xRot
    ) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        var player = level.getEntity(state.id);
        if (player == null || !SpecialPlayerPerks.hasKonEars(player.getUUID()) || state.isInvisible) {
            return;
        }
        poseStack.pushPose();
        getParentModel().getHead().translateAndRotate(poseStack);
        poseStack.translate(0.0d, -0.02d, 0.0d);
        coloredCutoutModelCopyLayerRender(
                ears, KonEarsModel.TEXTURE, poseStack, submitNodeCollector,
                lightCoords, state, 0xFFFFFFFF, 0);
        poseStack.popPose();
    }
}
