package com.koncompanions.client.renderer;

import com.koncompanions.client.FabricCompanionSkinTextures;
import com.koncompanions.client.model.FeminineCompanionModel;
import com.koncompanions.entity.FabricCompanionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class FabricCompanionRenderer
        extends MobRenderer<FabricCompanionEntity, FeminineCompanionModel<FabricCompanionEntity>> {
    private final FeminineCompanionModel<FabricCompanionEntity> wideModel;
    private final FeminineCompanionModel<FabricCompanionEntity> slimModel;

    public FabricCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new FeminineCompanionModel<>(context.bakeLayer(FeminineCompanionModel.LAYER_WIDE), false), 0.5f);
        this.wideModel = this.getModel();
        this.slimModel = new FeminineCompanionModel<>(context.bakeLayer(FeminineCompanionModel.LAYER_SLIM), true);
    }

    @Override
    public void render(FabricCompanionEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        this.model = entity.isSlimArms() ? slimModel : wideModel;
        this.shadowRadius = 0.5f * entity.getBodyScale();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FabricCompanionEntity entity) {
        return FabricCompanionSkinTextures.resolve(entity.getSkinPath());
    }
}
