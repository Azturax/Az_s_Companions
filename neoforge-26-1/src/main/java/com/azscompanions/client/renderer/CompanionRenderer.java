package com.azscompanions.client.renderer;

import com.azscompanions.client.ClientAppearanceDraft;
import com.azscompanions.client.CompanionSkinTextures;
import com.azscompanions.client.model.FeminineCompanionModel;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionMode;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/**
 * NeoForge 26.2 companion renderer (AvatarRenderState / submit pipeline).
 * Feminine mesh + proportions are live; armor/cape layers and mob-form rendering remain pending.
 */
public final class CompanionRenderer
        extends MobRenderer<CompanionEntity, CompanionRenderState, FeminineCompanionModel> {

    private static final Identifier FALLBACK_SKIN =
            Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private final FeminineCompanionModel wideModel;
    private final FeminineCompanionModel slimModel;
    @SuppressWarnings("unused") // reserved until SubmitNodeCollector mob-form port lands
    private final CompanionMobFormRenderer formRenderer;

    public CompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new FeminineCompanionModel(context.bakeLayer(FeminineCompanionModel.LAYER_WIDE), false), 0.5f);
        this.wideModel = this.getModel();
        this.slimModel = new FeminineCompanionModel(context.bakeLayer(FeminineCompanionModel.LAYER_SLIM), true);
        this.formRenderer = new CompanionMobFormRenderer(context);
    }

    @Override
    public CompanionRenderState createRenderState() {
        return new CompanionRenderState();
    }

    @Override
    public void extractRenderState(CompanionEntity entity, CompanionRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        CompanionForm form = entity.getForm();
        boolean slim = entity.isSlimArms();
        float scale = entity.getBodyScale();
        boolean showArmor = entity.isArmorVisible();
        boolean showNameTag = entity.isNameTagVisible();
        float bust = entity.getBust();
        float waist = entity.getWaist();
        float hips = entity.getHips();
        float shoulders = entity.getShoulders();
        float bustOffset = entity.getBustOffset();
        boolean showBust = entity.getGender().showsBust();
        CompanionMode mode = entity.getMode();

        if (ClientAppearanceDraft.matches(entity)) {
            ClientAppearanceDraft d = ClientAppearanceDraft.ACTIVE;
            if (d.form != null) {
                form = d.form;
            }
            slim = d.slimArms;
            scale = d.scale;
            showArmor = d.showArmor;
            showNameTag = d.showNameTag;
            bust = d.bust;
            waist = d.waist;
            hips = d.hips;
            shoulders = d.shoulders;
            bustOffset = d.bustOffset;
            showBust = d.gender.showsBust();
        }

        state.form = form;
        state.mode = mode;
        state.slimArms = slim;
        state.bodyScale = scale;
        state.showArmor = showArmor;
        state.showNameTag = showNameTag;
        state.bust = bust;
        state.waist = waist;
        state.hips = hips;
        state.shoulders = shoulders;
        state.bustOffset = bustOffset;
        state.showBust = showBust;
        state.passengerSitPose = mode == CompanionMode.SIT && form.usesPassengerSitPose();
        state.skinTexture = CompanionSkinTextures.resolve(entity);
        state.capeTexture = CompanionSkinTextures.resolveCape(entity);
        state.scale = scale;

        // Swap slim/wide before model setupAnim during submit.
        this.model = slim ? slimModel : wideModel;
        this.shadowRadius = form.isPlayer() ? 0.5f * scale : 0.4f * scale;
    }

    @Override
    public Identifier getTextureLocation(CompanionRenderState state) {
        if (state.skinTexture != null) {
            return state.skinTexture;
        }
        return FALLBACK_SKIN;
    }
}
