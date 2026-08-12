package com.azscompanions.client.model;

import com.azscompanions.AzsCompanions;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Small fox/Kon-style ears parented to the player head. */
public final class KonEarsModel extends EntityModel<AvatarRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "kon_ears"), "main");
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            AzsCompanions.MOD_ID, "textures/entity/kon_ears.png");

    public KonEarsModel(ModelPart root) {
        super(root);
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

}
