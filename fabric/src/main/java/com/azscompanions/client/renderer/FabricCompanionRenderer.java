package com.azscompanions.client.renderer;

import com.azscompanions.client.FabricClientAppearanceDraft;
import com.azscompanions.client.FabricCompanionSkinTextures;
import com.azscompanions.client.model.FeminineCompanionModel;
import com.azscompanions.compat.fancyanim.FancyAnimCompat;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.inventory.FabricCompanionInventory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class FabricCompanionRenderer
        extends MobRenderer<FabricCompanionEntity, FeminineCompanionModel<FabricCompanionEntity>> {
    private final FeminineCompanionModel<FabricCompanionEntity> wideModel;
    private final FeminineCompanionModel<FabricCompanionEntity> slimModel;
    private final CompanionMobFormRenderer formRenderer;

    public FabricCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new FeminineCompanionModel<>(context.bakeLayer(FeminineCompanionModel.LAYER_WIDE), false), 0.5f);
        this.wideModel = this.getModel();
        this.slimModel = new FeminineCompanionModel<>(context.bakeLayer(FeminineCompanionModel.LAYER_SLIM), true);
        this.formRenderer = new CompanionMobFormRenderer(context);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, FabricCompanionEntity entity,
                               float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                               float netHeadYaw, float headPitch) {
                if (!shouldRenderArmor(entity)) {
                    return;
                }
                super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTick,
                        ageInTicks, netHeadYaw, headPitch);
            }
        });
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        this.addLayer(new ElytraLayer<>(this, context.getModelSet()) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, FabricCompanionEntity entity,
                               float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                               float netHeadYaw, float headPitch) {
                if (!shouldRenderArmor(entity)) {
                    return;
                }
                super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTick,
                        ageInTicks, netHeadYaw, headPitch);
            }
        });
        this.addLayer(new CompanionCapeLayer(this));
    }

    private static boolean shouldRenderArmor(FabricCompanionEntity entity) {
        if (FabricClientAppearanceDraft.matches(entity)) {
            return FabricClientAppearanceDraft.ACTIVE.showArmor;
        }
        return entity.isArmorVisible();
    }

    @Override
    public void render(FabricCompanionEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        CompanionForm form = entity.getForm();
        if (FabricClientAppearanceDraft.matches(entity) && FabricClientAppearanceDraft.ACTIVE.form != null) {
            form = FabricClientAppearanceDraft.ACTIVE.form;
        }
        if (form.isOrb()) {
            this.shadowRadius = 0.15f * entity.getBodyScale();
            int color = entity.getOrbColorRgb();
            float amp = entity.getOrbFloatAmplitude();
            float speed = entity.getOrbFloatSpeed();
            float scale = entity.getBodyScale();
            if (FabricClientAppearanceDraft.matches(entity)) {
                color = FabricClientAppearanceDraft.ACTIVE.orbColorRgb;
                amp = FabricClientAppearanceDraft.ACTIVE.orbFloatAmplitude;
                speed = FabricClientAppearanceDraft.ACTIVE.orbFloatSpeed;
                scale = FabricClientAppearanceDraft.ACTIVE.scale;
            }
            CompanionOrbRenderer.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight,
                    color, amp, speed, scale);
            if (this.shouldShowName(entity)) {
                this.renderNameTag(entity, entity.getDisplayName(), poseStack, buffer, packedLight, partialTicks);
            }
            return;
        }
        if (!form.isPlayer()) {
            this.shadowRadius = 0.4f * entity.getBodyScale();
            formRenderer.render(entity, form, entityYaw, partialTicks, poseStack, buffer, packedLight);
            if (this.shouldShowName(entity)) {
                this.renderNameTag(entity, entity.getDisplayName(), poseStack, buffer, packedLight, partialTicks);
            }
            return;
        }
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
    protected boolean shouldShowName(FabricCompanionEntity entity) {
        if (FabricClientAppearanceDraft.matches(entity)) {
            if (!FabricClientAppearanceDraft.ACTIVE.showNameTag) {
                return false;
            }
        } else if (!entity.isNameTagVisible()) {
            return false;
        }
        return super.shouldShowName(entity);
    }

    /**
     * Nametag Y from the form currently being rendered (including Customize draft), every frame —
     * never reuse a previous form's attachment height after chicken↔player swaps.
     */
    @Override
    protected void renderNameTag(FabricCompanionEntity entity, Component displayName, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight, float partialTick) {
        Vec3 attachment = entity.getAttachments().getNullable(
                EntityAttachment.NAME_TAG, 0, entity.getViewYRot(partialTick));
        double currentY = attachment != null ? attachment.y : entity.getBbHeight();
        double desiredY = resolveNameTagBodyHeight(entity);
        poseStack.pushPose();
        poseStack.translate(0.0, desiredY - currentY, 0.0);
        super.renderNameTag(entity, displayName, poseStack, bufferSource, packedLight, partialTick);
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
    public ResourceLocation getTextureLocation(FabricCompanionEntity entity) {
        return FabricCompanionSkinTextures.resolve(entity);
    }

    /**
     * Default: {@link RenderType#entityCutoutNoCull} so Kon / Mojang skins stay visible.
     * Do not call {@code super} here — {@link FeminineCompanionModel} extends {@code PlayerModel},
     * whose model render-type is translucent and can draw fully invisible on Iris/Sodium without ETF.
     * Translucent only when Fancy Anim config allows it <em>and</em> EMF/ETF is present.
     * <p>
     * Match vanilla {@code LivingEntityRenderer} order: draw the body when visible; use
     * {@link RenderType#outline} only when the body is hidden but the entity should still glow.
     * Checking {@code glowing} first (0.3.7–0.3.8) made Glowing companions outline-only.
     */
    @Override
    @Nullable
    protected RenderType getRenderType(FabricCompanionEntity entity, boolean bodyVisible, boolean translucent,
                                       boolean glowing) {
        ResourceLocation texture = this.getTextureLocation(entity);
        if (bodyVisible || translucent) {
            if (FancyAnimCompat.useTranslucentPlayerSkins()) {
                return RenderType.entityTranslucent(texture);
            }
            return RenderType.entityCutoutNoCull(texture);
        }
        return glowing ? RenderType.outline(texture) : null;
    }

    private static final class CompanionCapeLayer
            extends RenderLayer<FabricCompanionEntity, FeminineCompanionModel<FabricCompanionEntity>> {
        CompanionCapeLayer(FabricCompanionRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, FabricCompanionEntity entity,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            if (!entity.getForm().isPlayer() || entity.isInvisible()) {
                return;
            }
            ResourceLocation cape = FabricCompanionSkinTextures.resolveCape(entity);
            if (cape == null) {
                return;
            }
            if (entity.getCompanionInventory().getItem(FabricCompanionInventory.CHEST).is(Items.ELYTRA)
                    && shouldRenderArmor(entity)) {
                return;
            }
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, 0.125F);
            Vec3 delta = entity.getDeltaMovement();
            float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
            double sin = Mth.sin(bodyYaw * ((float) Math.PI / 180F));
            double cos = -Mth.cos(bodyYaw * ((float) Math.PI / 180F));
            float bob = Mth.clamp((float) (delta.y * 10.0F), -6.0F, 32.0F);
            float flare = Mth.clamp((float) (delta.x * sin + delta.z * cos) * 100.0F, 0.0F, 150.0F);
            float side = Mth.clamp((float) (delta.x * cos - delta.z * sin) * 100.0F, -20.0F, 20.0F);
            bob += Mth.sin(limbSwing * 6.0F) * 32.0F * limbSwingAmount;
            if (entity.isCrouching()) {
                bob += 25.0F;
            }
            poseStack.mulPose(Axis.XP.rotationDegrees(6.0F + flare / 2.0F + bob));
            poseStack.mulPose(Axis.ZP.rotationDegrees(side / 2.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - side / 2.0F));
            RenderType capeType = FancyAnimCompat.useTranslucentPlayerSkins()
                    ? RenderType.entityTranslucent(cape)
                    : RenderType.entitySolid(cape);
            VertexConsumer consumer = buffer.getBuffer(capeType);
            getParentModel().renderCloak(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }
}
