package com.azscompanions.client.renderer;

import com.azscompanions.entity.CompanionEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Minimal NeoForge 26.2 companion renderer (HumanoidRenderState / submit pipeline).
 * Full feminine mesh, armor/cape layers, and mob-form rendering remain pending.
 */
public final class CompanionRenderer
        extends MobRenderer<CompanionEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier FALLBACK_SKIN =
            Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");

    public CompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return FALLBACK_SKIN;
    }

    @Override
    public void extractRenderState(CompanionEntity entity, HumanoidRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.scale = entity.getBodyScale();
    }
}
