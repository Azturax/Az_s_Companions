package com.azscompanions.client.model;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.entity.CompanionBodyProportions;
import com.azscompanions.entity.FabricCompanionEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Vanilla-style companion mesh; mirrors NeoForge (player layout + simple bust). */
public final class FeminineCompanionModel<T extends LivingEntity> extends PlayerModel<T> {
    public static final ModelLayerLocation LAYER_WIDE = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "feminine_companion"), "main");
    public static final ModelLayerLocation LAYER_SLIM = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "feminine_companion_slim"), "main");

    private static final float BUST_PIVOT_Y = 2.0F;
    private static final float BUST_X_ROT = (float) Math.toRadians(45.0);

    private final ModelPart bust;

    public FeminineCompanionModel(ModelPart root, boolean slim) {
        super(root, slim);
        this.bust = this.body.getChild("bust");
    }

    public static LayerDefinition createBodyLayer(boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        PartDefinition body = mesh.getRoot().getChild("body");

        body.addOrReplaceChild("bust", CubeListBuilder.create()
                        .texOffs(20, 21)
                        .addBox(-3.5F, -0.2F, -2.6F, 3.0F, 3.0F, 2.4F, CubeDeformation.NONE)
                        .texOffs(20, 21)
                        .addBox(0.5F, -0.2F, -2.6F, 3.0F, 3.0F, 2.4F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, BUST_PIVOT_Y, 0.0F, BUST_X_ROT, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        resetPartScales();
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        boolean showBust = true;
        if (entity instanceof FabricCompanionEntity companion) {
            showBust = companion.getGender().showsBust();
            applyProportions(companion.getBust(), companion.getWaist(), companion.getHips(),
                    companion.getShoulders(), companion.getBustOffset(), showBust);
        } else {
            applyProportions(
                    CompanionBodyProportions.DEFAULT_BUST,
                    CompanionBodyProportions.DEFAULT_WAIST,
                    CompanionBodyProportions.DEFAULT_HIPS,
                    CompanionBodyProportions.DEFAULT_SHOULDERS,
                    CompanionBodyProportions.DEFAULT_BUST_OFFSET,
                    true);
        }
        this.bust.visible = showBust && this.body.visible;
        this.bust.xRot = BUST_X_ROT;
    }

    private void resetPartScales() {
        this.body.xScale = this.body.yScale = this.body.zScale = 1.0f;
        this.bust.xScale = this.bust.yScale = this.bust.zScale = 1.0f;
        this.leftArm.xScale = this.leftArm.yScale = this.leftArm.zScale = 1.0f;
        this.rightArm.xScale = this.rightArm.yScale = this.rightArm.zScale = 1.0f;
        this.leftLeg.xScale = this.leftLeg.yScale = this.leftLeg.zScale = 1.0f;
        this.rightLeg.xScale = this.rightLeg.yScale = this.rightLeg.zScale = 1.0f;
        this.bust.y = BUST_PIVOT_Y;
        this.bust.z = 0.0f;
        this.bust.xRot = BUST_X_ROT;
    }

    public void applyProportions(float bustSize, float waist, float hipSize, float shoulders, float bustOffset,
                                 boolean showBust) {
        float b = CompanionBodyProportions.clampBust(bustSize);
        float w = CompanionBodyProportions.clampWaist(waist);
        float h = CompanionBodyProportions.clampHips(hipSize);
        float s = CompanionBodyProportions.clampShoulders(shoulders);
        float o = CompanionBodyProportions.clampBustOffset(bustOffset);

        this.body.xScale = 0.96f + (w - 1.0f) * 0.35f;
        this.body.zScale = 0.98f + (w - 1.0f) * 0.15f;
        if (showBust) {
            this.bust.xScale = 0.92f + (b - 1.0f) * 0.8f;
            this.bust.yScale = 0.94f + (b - 1.0f) * 0.5f;
            this.bust.zScale = 0.92f + (b - 1.0f) * 0.85f;
            this.bust.y = BUST_PIVOT_Y + o * 0.12f;
            this.bust.z = -o * 0.75f;
            this.bust.xRot = BUST_X_ROT;
        } else {
            this.bust.xScale = this.bust.yScale = this.bust.zScale = 0.0f;
            this.bust.y = BUST_PIVOT_Y;
            this.bust.z = 0.0f;
            this.bust.xRot = BUST_X_ROT;
        }

        float legX = 0.98f + (h - 1.0f) * 0.35f;
        this.leftLeg.xScale = legX;
        this.rightLeg.xScale = legX;
        this.leftArm.xScale = 0.97f + (s - 1.0f) * 0.45f;
        this.rightArm.xScale = this.leftArm.xScale;
        if (this.leftSleeve != null) {
            this.leftSleeve.xScale = this.leftArm.xScale;
            this.rightSleeve.xScale = this.rightArm.xScale;
        }
        if (this.leftPants != null) {
            this.leftPants.xScale = this.leftLeg.xScale;
            this.rightPants.xScale = this.rightLeg.xScale;
        }
    }
}
