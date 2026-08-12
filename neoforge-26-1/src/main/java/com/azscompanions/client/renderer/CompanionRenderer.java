package com.azscompanions.client.renderer;

import com.azscompanions.client.ClientAppearanceDraft;
import com.azscompanions.client.CompanionSkinTextures;
import com.azscompanions.client.model.FeminineCompanionModel;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionMode;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;

/**
 * NeoForge 26.2 companion renderer (AvatarRenderState / submit pipeline).
 * Supports feminine proportions, equipment layers, player skins/capes, and delegated mob forms.
 */
public final class CompanionRenderer
        extends MobRenderer<CompanionEntity, CompanionRenderState, FeminineCompanionModel> {

    private static final Identifier FALLBACK_SKIN =
            Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private final FeminineCompanionModel wideModel;
    private final FeminineCompanionModel slimModel;
    private final CompanionMobFormRenderer formRenderer;

    public CompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new FeminineCompanionModel(context.bakeLayer(FeminineCompanionModel.LAYER_WIDE), false), 0.5f);
        this.wideModel = this.getModel();
        this.slimModel = new FeminineCompanionModel(context.bakeLayer(FeminineCompanionModel.LAYER_SLIM), true);
        this.formRenderer = new CompanionMobFormRenderer(context);
        this.addLayer(new HumanoidArmorLayer(
                this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), part -> new PlayerModel(part, false)),
                context.getEquipmentRenderer()));
        this.addLayer(new ItemInHandLayer(this));
        this.addLayer(new WingsLayer(this, context.getModelSet(), context.getEquipmentRenderer()));
        this.addLayer((net.minecraft.client.renderer.entity.layers.RenderLayer) new CapeLayer(
                (net.minecraft.client.renderer.entity.RenderLayerParent) this,
                context.getModelSet(),
                context.getEquipmentAssets()));
    }

    @Override
    public CompanionRenderState createRenderState() {
        return new CompanionRenderState();
    }

    @Override
    public void extractRenderState(CompanionEntity entity, CompanionRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, this.itemModelResolver);
        state.source = entity;

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
        ClientAsset.Texture body = new ClientAsset.DownloadedTexture(
                state.skinTexture != null ? state.skinTexture : FALLBACK_SKIN, "");
        ClientAsset.Texture cape = state.capeTexture == null
                ? null
                : new ClientAsset.DownloadedTexture(state.capeTexture, "");
        state.skin = new PlayerSkin(
                body, cape, cape, slim ? PlayerModelType.SLIM : PlayerModelType.WIDE, false);
        state.showCape = cape != null;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        if (!showArmor) {
            state.headEquipment = ItemStack.EMPTY;
            state.chestEquipment = ItemStack.EMPTY;
            state.legsEquipment = ItemStack.EMPTY;
            state.feetEquipment = ItemStack.EMPTY;
        }
        state.scale = scale;

        // Swap slim/wide before model setupAnim during submit.
        this.model = slim ? slimModel : wideModel;
        this.shadowRadius = form.isPlayer() ? 0.5f * scale : 0.4f * scale;
    }

    @Override
    public void submit(
            CompanionRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        if (state.form != null && !state.form.isPlayer()) {
            formRenderer.submit(state.source, state.form, state.partialTick, poseStack, submitNodeCollector, camera);
            return;
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    @Override
    public Identifier getTextureLocation(CompanionRenderState state) {
        if (state.skinTexture != null) {
            return state.skinTexture;
        }
        return FALLBACK_SKIN;
    }
}
