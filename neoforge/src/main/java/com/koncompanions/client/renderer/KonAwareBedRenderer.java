package com.koncompanions.client.renderer;

import com.koncompanions.KonCompanions;
import com.koncompanions.block.KonBedBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Vanilla bed renderer that swaps in the Kon bed sheet when the block is {@link KonBedBlock}.
 */
public final class KonAwareBedRenderer implements BlockEntityRenderer<BedBlockEntity> {
    private static final Material KON_BED_MATERIAL = new Material(
            Sheets.BED_SHEET,
            ResourceLocation.fromNamespaceAndPath(KonCompanions.MOD_ID, "entity/bed/kon"));

    private final ModelPart headRoot;
    private final ModelPart footRoot;

    public KonAwareBedRenderer(BlockEntityRendererProvider.Context context) {
        this.headRoot = context.bakeLayer(ModelLayers.BED_HEAD);
        this.footRoot = context.bakeLayer(ModelLayers.BED_FOOT);
    }

    @Override
    public void render(BedBlockEntity bed, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Material material = bed.getBlockState().getBlock() instanceof KonBedBlock
                ? KON_BED_MATERIAL
                : Sheets.BED_TEXTURES[bed.getColor().ordinal()];
        Level level = bed.getLevel();
        if (level != null) {
            BlockState state = bed.getBlockState();
            DoubleBlockCombiner.NeighborCombineResult<? extends BedBlockEntity> neighbors =
                    DoubleBlockCombiner.combineWithNeigbour(
                            BlockEntityType.BED,
                            BedBlock::getBlockType,
                            BedBlock::getConnectedDirection,
                            ChestBlock.FACING,
                            state,
                            level,
                            bed.getBlockPos(),
                            (lvl, pos) -> false);
            int light = neighbors.apply(new BrightnessCombiner<>()).applyAsInt(packedLight);
            renderPiece(
                    poseStack,
                    buffer,
                    state.getValue(BedBlock.PART) == BedPart.HEAD ? headRoot : footRoot,
                    state.getValue(BedBlock.FACING),
                    material,
                    light,
                    packedOverlay,
                    false);
        } else {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, -1.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            renderPiece(poseStack, buffer, headRoot, Direction.SOUTH, material, packedLight, packedOverlay, false);
            poseStack.translate(0.0F, 1.0F, 0.0F);
            renderPiece(poseStack, buffer, footRoot, Direction.SOUTH, material, packedLight, packedOverlay, true);
            poseStack.popPose();
        }
    }

    private void renderPiece(PoseStack poseStack, MultiBufferSource buffer, ModelPart modelPart,
                             Direction direction, Material material, int packedLight, int packedOverlay,
                             boolean footOffset) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5625F, footOffset ? -1.0F : 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + direction.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        VertexConsumer consumer = material.buffer(buffer, RenderType::entitySolid);
        modelPart.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
