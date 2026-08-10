package com.azscompanions.client.renderer;

import com.azscompanions.block.KonBedBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

/**
 * Placeholder Kon bed BER for NeoForge 26.2 (BlockEntityRenderState pipeline).
 */
public final class KonAwareBedRenderer implements BlockEntityRenderer<KonBedBlockEntity, BlockEntityRenderState> {
    public KonAwareBedRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }

    @Override
    public void submit(
            BlockEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        // Full Kon bed mesh pending Material/submit port.
    }
}
