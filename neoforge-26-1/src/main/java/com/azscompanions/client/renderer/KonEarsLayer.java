package com.azscompanions.client.renderer;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.model.player.PlayerModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;

/**
 * Kon ears layer stub — full ears mesh pending PlayerModel/AvatarRenderState port.
 */
public final class KonEarsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public KonEarsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models) {
        super(parent);
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
        // No-op until ears model is ported.
    }
}
