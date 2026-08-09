package com.azscompanions.client.renderer;

import com.azscompanions.AzsCompanions;
import com.azscompanions.block.KonBedBlockEntity;
import com.azscompanions.registry.ModBlockEntities;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Renders only {@link KonBedBlockEntity}. Vanilla beds keep {@code BedRenderer} on {@code BlockEntityType.BED}.
 */
public final class KonAwareBedRenderer implements BlockEntityRenderer<KonBedBlockEntity> {
    private static final Material KON_BED_MATERIAL = new Material(
            Sheets.BED_SHEET,
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "entity/bed/kon"));

    private final ModelPart headRoot;
    private final ModelPart footRoot;

    public KonAwareBedRenderer(BlockEntityRendererProvider.Context context) {
        this.headRoot = context.bakeLayer(ModelLayers.BED_HEAD);
        this.footRoot = context.bakeLayer(ModelLayers.BED_FOOT);
    }

    @Override
    public void render(KonBedBlockEntity bed, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = bed.getLevel();
        if (level == null) {
            // Kon bed item uses a flat generated model; no inventory BER path.
            return;
        }
        BlockState state = bed.getBlockState();
        DoubleBlockCombiner.NeighborCombineResult<? extends KonBedBlockEntity> neighbors =
                DoubleBlockCombiner.combineWithNeigbour(
                        ModBlockEntities.KON_BED.get(),
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
                KON_BED_MATERIAL,
                light,
                packedOverlay,
                false);
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
