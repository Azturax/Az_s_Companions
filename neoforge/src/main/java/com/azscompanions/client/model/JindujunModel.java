package com.azscompanions.client.model;

import com.azscompanions.AzsCompanionsConstants;
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
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Flying Nimbus / Jindujun cloud mesh — ported from Desktop Blockbench export
 * {@code Jindujun.java} (1.17+ Mojmap) to 1.21.1 ModelPart baking.
 */
public final class JindujunModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AzsCompanionsConstants.MOD_ID, "jindujun"), "main");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AzsCompanionsConstants.MOD_ID, "textures/entity/companion/jindujun.png");

    private final ModelPart root;
    private final ModelPart bbMain;

    public JindujunModel(ModelPart root) {
        this.root = root;
        this.bbMain = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition part = mesh.getRoot();

        part.addOrReplaceChild("bb_main", CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-3.0F, -2.0F, -6.0F, 7.0F, 2.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-4.0F, -5.0F, -7.0F, 9.0F, 3.0F, 14.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 31).addBox(-3.0F, -6.0F, -6.0F, 7.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 17).addBox(5.0F, -4.0F, -5.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 35).addBox(5.0F, -4.0F, 0.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 39).addBox(-5.0F, -4.0F, 4.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(6, 44).addBox(-2.0F, -4.0F, 7.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 29).addBox(1.0F, -4.0F, 7.0F, 4.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 43).addBox(5.0F, -4.0F, 5.0F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                        .texOffs(10, 44).addBox(-5.0F, -4.0F, 2.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 23).addBox(-5.0F, -4.0F, -6.0F, 1.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(38, 32).addBox(-3.0F, -4.0F, -8.0F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 44).addBox(2.0F, -4.0F, -8.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay) {
        bbMain.render(poseStack, consumer, packedLight, packedOverlay);
    }

    public void render(PoseStack poseStack, VertexConsumer consumer, int packedLight) {
        render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
    }

    public ModelPart root() {
        return root;
    }
}
