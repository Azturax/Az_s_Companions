package com.azscompanions.client.renderer;

import com.azscompanions.client.ClientAppearanceDraft;
import com.azscompanions.client.CompanionSkinTextures;
import com.azscompanions.client.model.FeminineCompanionModel;
import com.azscompanions.entity.CompanionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders companions with an adult feminine player-derived model.
 * Skins are classic Minecraft 64×64 player skins (resource location or local import).
 * Held items use {@link ItemInHandLayer} and scale with body {@code SCALE}.
 */
public final class CompanionRenderer extends MobRenderer<CompanionEntity, FeminineCompanionModel<CompanionEntity>> {
    private final FeminineCompanionModel<CompanionEntity> wideModel;
    private final FeminineCompanionModel<CompanionEntity> slimModel;

    public CompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new FeminineCompanionModel<>(context.bakeLayer(FeminineCompanionModel.LAYER_WIDE), false), 0.5f);
        this.wideModel = this.getModel();
        this.slimModel = new FeminineCompanionModel<>(context.bakeLayer(FeminineCompanionModel.LAYER_SLIM), true);
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(CompanionEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        boolean slim = entity.isSlimArms();
        float scale = entity.getBodyScale();
        if (ClientAppearanceDraft.matches(entity)) {
            slim = ClientAppearanceDraft.ACTIVE.slimArms;
            scale = ClientAppearanceDraft.ACTIVE.scale;
        }
        this.model = slim ? slimModel : wideModel;
        this.shadowRadius = 0.5f * scale;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CompanionEntity entity) {
        return CompanionSkinTextures.resolve(entity);
    }
}
