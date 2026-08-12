package com.azscompanions.client.renderer;

import com.azscompanions.AzsCompanions;
import com.azscompanions.block.KonBedBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BedRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Renders only {@link KonBedBlockEntity}. Vanilla beds use their normal block models.
 */
public final class KonAwareBedRenderer implements BlockEntityRenderer<KonBedBlockEntity, BedRenderState> {
    public static final ModelLayerLocation HEAD_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "kon_bed_head"), "main");
    public static final ModelLayerLocation FOOT_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "kon_bed_foot"), "main");
    private static final SpriteId KON_BED_SPRITE = Sheets.BLOCK_ENTITIES_MAPPER.apply(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "bed/kon"));

    private final ModelPart headRoot;
    private final ModelPart footRoot;
    private final SpriteGetter sprites;

    public KonAwareBedRenderer(BlockEntityRendererProvider.Context context) {
        this.headRoot = context.bakeLayer(HEAD_LAYER);
        this.footRoot = context.bakeLayer(FOOT_LAYER);
        this.sprites = context.sprites();
    }

    public static LayerDefinition createHeadLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("main",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 6.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(50, 0).addBox(0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(50, 0).addBox(0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(13.0F, 6.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createFootLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("main",
                CubeListBuilder.create().texOffs(0, 22).addBox(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 6.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(50, 6).addBox(0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 6.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(50, 6).addBox(0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(13.0F, 6.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public BedRenderState createRenderState() {
        return new BedRenderState();
    }

    @Override
    public void extractRenderState(
            KonBedBlockEntity bed,
            BedRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(bed, state, partialTick, cameraPosition, breakProgress);
        BlockState blockState = bed.getBlockState();
        state.facing = blockState.getValue(BedBlock.FACING);
        state.part = blockState.getValue(BedBlock.PART);
    }

    @Override
    public void submit(
            BedRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        ModelPart model = state.part == net.minecraft.world.level.block.state.properties.BedPart.HEAD
                ? headRoot
                : footRoot;
        renderPiece(poseStack, submitNodeCollector, model, state.facing, state.lightCoords, state.breakProgress);
    }

    private void renderPiece(
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            ModelPart model,
            Direction direction,
            int packedLight,
            ModelFeatureRenderer.CrumblingOverlay breakProgress
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5625F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + direction.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        submitNodeCollector.submitModelPart(
                model,
                poseStack,
                KON_BED_SPRITE.renderType(RenderTypes::entitySolid),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                sprites.get(KON_BED_SPRITE),
                -1,
                breakProgress);
        poseStack.popPose();
    }
}
