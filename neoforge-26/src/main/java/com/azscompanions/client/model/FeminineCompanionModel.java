package com.azscompanions.client.model;

import com.azscompanions.AzsCompanions;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.Identifier;

/**
 * Companion mesh stub for NeoForge 26.2 — uses vanilla PlayerModel (AvatarRenderState).
 * Bust/proportion deformation from 1.21.1 is pending port onto the new anim pipeline.
 */
public final class FeminineCompanionModel extends PlayerModel {
    public static final ModelLayerLocation LAYER_WIDE = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "feminine_companion"), "main");
    public static final ModelLayerLocation LAYER_SLIM = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "feminine_companion_slim"), "main");

    public FeminineCompanionModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    public static LayerDefinition createBodyLayer(boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, slim);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
