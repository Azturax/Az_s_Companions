package com.koncompanions.client.renderer;

import com.koncompanions.client.CompanionSkinTextures;
import com.koncompanions.client.model.FeminineCompanionModel;
import com.koncompanions.entity.CompanionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders companions with an adult feminine player-derived model.
 * Skins are classic Minecraft 64×64 player skins (resource location or local import).
 */
public final class CompanionRenderer extends MobRenderer<CompanionEntity, FeminineCompanionModel<CompanionEntity>> {
    private final FeminineCompanionModel<CompanionEntity> wideModel;
    private final FeminineCompanionModel<CompanionEntity> slimModel;

    public CompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new FeminineCompanionModel<>(context.bakeLayer(FeminineCompanionModel.LAYER_WIDE), false), 0.5f);
        this.wideModel = this.getModel();
        this.slimModel = new FeminineCompanionModel<>(context.bakeLayer(FeminineCompanionModel.LAYER_SLIM), true);
    }

    @Override
    public void render(CompanionEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        this.model = entity.isSlimArms() ? slimModel : wideModel;
        this.shadowRadius = 0.5f * entity.getBodyScale();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CompanionEntity entity) {
        return CompanionSkinTextures.resolve(entity);
    }
}
