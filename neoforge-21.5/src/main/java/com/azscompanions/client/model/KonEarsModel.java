package com.azscompanions.client.model;

import com.azscompanions.AzsCompanions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Small fox/Kon-style ears parented to the player head.
 * Texture: lavender outer (u 0–7) + light-blue inner (u 8–15).
 */
public final class KonEarsModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "kon_ears"), "main");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AzsCompanions.MOD_ID, "textures/entity/kon_ears.png");

    private final ModelPart root;

    public KonEarsModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("left_ear",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -3.5F, -0.5F, 2.0F, 3.5F, 1.0F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(2.2F, -6.0F, 0.0F, 0.0F, 0.0F, 0.28F));
        root.addOrReplaceChild("right_ear",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0F, -3.5F, -0.5F, 2.0F, 3.5F, 1.0F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(-2.2F, -6.0F, 0.0F, 0.0F, 0.0F, -0.28F));

        root.addOrReplaceChild("left_ear_inner",
                CubeListBuilder.create()
                        .texOffs(8, 0)
                        .addBox(-0.5F, -2.8F, -0.65F, 1.0F, 2.2F, 0.5F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(2.2F, -6.0F, 0.0F, 0.0F, 0.0F, 0.28F));
        root.addOrReplaceChild("right_ear_inner",
                CubeListBuilder.create()
                        .texOffs(8, 0)
                        .addBox(-0.5F, -2.8F, -0.65F, 1.0F, 2.2F, 0.5F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.2F, -6.0F, 0.0F, 0.0F, 0.0F, -0.28F));

        return LayerDefinition.create(mesh, 16, 16);
    }

    public void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        root.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
