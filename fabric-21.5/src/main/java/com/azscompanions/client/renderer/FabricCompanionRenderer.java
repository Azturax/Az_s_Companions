package com.azscompanions.client.renderer;

import com.azscompanions.client.FabricClientAppearanceDraft;
import com.azscompanions.client.FabricCompanionSkinTextures;
import com.azscompanions.client.model.FeminineCompanionModel;
import com.azscompanions.compat.fancyanim.FancyAnimCompat;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerCapeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Humanoid companions + optional Mojang cape; non-player forms use {@link CompanionMobFormRenderer}.
 */
public final class FabricCompanionRenderer
        extends MobRenderer<FabricCompanionEntity, PlayerRenderState, FeminineCompanionModel> {
    private final FeminineCompanionModel wideModel;
    private final FeminineCompanionModel slimModel;
    private final CompanionMobFormRenderer formRenderer;

    public FabricCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new FeminineCompanionModel(context.bakeLayer(FeminineCompanionModel.LAYER_WIDE), false), 0.5f);
        this.wideModel = this.getModel();
        this.slimModel = new FeminineCompanionModel(context.bakeLayer(FeminineCompanionModel.LAYER_SLIM), true);
        this.formRenderer = new CompanionMobFormRenderer(context);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getEquipmentRenderer()) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                               PlayerRenderState state, float yRot, float xRot) {
                if (state instanceof CompanionRenderState companion && !companion.showArmor) {
                    return;
                }
                super.render(poseStack, buffer, packedLight, state, yRot, xRot);
            }
        });
        this.addLayer(new ItemInHandLayer<>(this));
        this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                               PlayerRenderState state, float yRot, float xRot) {
                if (state instanceof CompanionRenderState companion && !companion.showArmor) {
                    return;
                }
                super.render(poseStack, buffer, packedLight, state, yRot, xRot);
            }
        });
        this.addLayer(new CompanionCapeLayer(this, context));
    }

    @Override
    public PlayerRenderState createRenderState() {
        return new CompanionRenderState();
    }

    @Override
    public void extractRenderState(FabricCompanionEntity entity, PlayerRenderState base, float partialTick) {
        super.extractRenderState(entity, base, partialTick);
        CompanionRenderState state = (CompanionRenderState) base;

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
        FabricCompanionMode mode = entity.getMode();

        if (FabricClientAppearanceDraft.matches(entity)) {
            FabricClientAppearanceDraft d = FabricClientAppearanceDraft.ACTIVE;
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
        state.passengerSitPose = mode == FabricCompanionMode.SIT && form.usesPassengerSitPose();
        state.skinTexture = FabricCompanionSkinTextures.resolve(entity);
        state.capeTexture = FabricCompanionSkinTextures.resolveCape(entity);
        state.showCape = state.capeTexture != null;
        state.scale = scale;

        state.headEquipment = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        state.chestEquipment = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        state.legsEquipment = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
        state.feetEquipment = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET);
    }

    @Override
    public void render(PlayerRenderState base, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        CompanionRenderState state = (CompanionRenderState) base;
        if (!state.form.isPlayer()) {
            this.shadowRadius = 0.4f * state.bodyScale;
            FabricCompanionEntity entity = resolveEntity(state);
            if (entity != null) {
                formRenderer.render(entity, state.form, state.yRot, 0.0f, poseStack, buffer, packedLight);
            }
            return;
        }

        this.model = state.slimArms ? slimModel : wideModel;
        this.shadowRadius = 0.5f * state.bodyScale;
        super.render(base, poseStack, buffer, packedLight);
    }

    @Nullable
    private static FabricCompanionEntity resolveEntity(CompanionRenderState state) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        var entity = level.getEntity(state.id);
        return entity instanceof FabricCompanionEntity companion ? companion : null;
    }

    @Override
    protected boolean shouldShowName(FabricCompanionEntity entity, double distanceToCameraSq) {
        if (FabricClientAppearanceDraft.matches(entity)) {
            if (!FabricClientAppearanceDraft.ACTIVE.showNameTag) {
                return false;
            }
        } else if (!entity.isNameTagVisible()) {
            return false;
        }
        return super.shouldShowName(entity, distanceToCameraSq);
    }

    @Override
    protected void renderNameTag(PlayerRenderState state, Component displayName, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight) {
        FabricCompanionEntity entity = resolveEntity((CompanionRenderState) state);
        if (entity == null) {
            super.renderNameTag(state, displayName, poseStack, bufferSource, packedLight);
            return;
        }
        Vec3 attachment = entity.getAttachments().getNullable(
                EntityAttachment.NAME_TAG, 0, entity.getYRot());
        double currentY = attachment != null ? attachment.y : entity.getBbHeight();
        double desiredY = resolveNameTagBodyHeight(entity);
        poseStack.pushPose();
        poseStack.translate(0.0, desiredY - currentY, 0.0);
        super.renderNameTag(state, displayName, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    private static float resolveNameTagBodyHeight(FabricCompanionEntity entity) {
        CompanionForm form = entity.getForm();
        float scale = entity.getBodyScale();
        if (FabricClientAppearanceDraft.matches(entity)) {
            if (FabricClientAppearanceDraft.ACTIVE.form != null) {
                form = FabricClientAppearanceDraft.ACTIVE.form;
            }
            scale = FabricClientAppearanceDraft.ACTIVE.scale;
        }
        return form.height() * scale;
    }

    @Override
    public ResourceLocation getTextureLocation(PlayerRenderState state) {
        if (state instanceof CompanionRenderState companion && companion.skinTexture != null) {
            return companion.skinTexture;
        }
        return ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
    }

    @Override
    @Nullable
    protected RenderType getRenderType(PlayerRenderState state, boolean bodyVisible, boolean translucent,
                                       boolean glowing) {
        ResourceLocation texture = this.getTextureLocation(state);
        if (bodyVisible || translucent) {
            if (FancyAnimCompat.useTranslucentPlayerSkins()) {
                return RenderType.entityTranslucent(texture);
            }
            return RenderType.entityCutoutNoCull(texture);
        }
        return glowing ? RenderType.outline(texture) : null;
    }

    private static final class CompanionCapeLayer
            extends RenderLayer<PlayerRenderState, FeminineCompanionModel> {
        private final PlayerCapeModel<PlayerRenderState> capeModel;

        CompanionCapeLayer(FabricCompanionRenderer parent, EntityRendererProvider.Context context) {
            super(parent);
            this.capeModel = new PlayerCapeModel<>(context.bakeLayer(ModelLayers.PLAYER_CAPE));
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                           PlayerRenderState state, float yRot, float xRot) {
            if (!(state instanceof CompanionRenderState companion)) {
                return;
            }
            if (!companion.form.isPlayer() || companion.isInvisible || companion.capeTexture == null) {
                return;
            }
            if (companion.chestEquipment.is(Items.ELYTRA) && companion.showArmor) {
                return;
            }

            poseStack.pushPose();
            getParentModel().copyPropertiesTo(capeModel);
            capeModel.setupAnim(companion);
            RenderType capeType = FancyAnimCompat.useTranslucentPlayerSkins()
                    ? RenderType.entityTranslucent(companion.capeTexture)
                    : RenderType.entitySolid(companion.capeTexture);
            VertexConsumer consumer = buffer.getBuffer(capeType);
            capeModel.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }
}
