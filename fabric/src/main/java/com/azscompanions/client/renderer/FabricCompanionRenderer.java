package com.azscompanions.client.renderer;

import com.azscompanions.client.FabricClientAppearanceDraft;
import com.azscompanions.client.FabricCompanionSkinTextures;
import com.azscompanions.client.model.FeminineCompanionModel;
import com.azscompanions.entity.FabricCompanionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public final class FabricCompanionRenderer
        extends MobRenderer<FabricCompanionEntity, FeminineCompanionModel<FabricCompanionEntity>> {
    private final FeminineCompanionModel<FabricCompanionEntity> wideModel;
    private final FeminineCompanionModel<FabricCompanionEntity> slimModel;

    public FabricCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new FeminineCompanionModel<>(context.bakeLayer(FeminineCompanionModel.LAYER_WIDE), false), 0.5f);
        this.wideModel = this.getModel();
        this.slimModel = new FeminineCompanionModel<>(context.bakeLayer(FeminineCompanionModel.LAYER_SLIM), true);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(FabricCompanionEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        boolean slim = entity.isSlimArms();
        float scale = entity.getBodyScale();
        if (FabricClientAppearanceDraft.matches(entity)) {
            slim = FabricClientAppearanceDraft.ACTIVE.slimArms;
            scale = FabricClientAppearanceDraft.ACTIVE.scale;
        }
        this.model = slim ? slimModel : wideModel;
        this.shadowRadius = 0.5f * scale;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FabricCompanionEntity entity) {
        return FabricCompanionSkinTextures.resolve(entity);
    }
}
