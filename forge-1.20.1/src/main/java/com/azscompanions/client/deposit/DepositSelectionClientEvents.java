package com.azscompanions.client.deposit;

import com.azscompanions.AzsCompanions;
import com.azscompanions.deposit.ClientDepositSelection;
import com.azscompanions.deposit.DepositChestRef;
import com.azscompanions.network.packet.DepositExitModePacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.client.gui.screens.PauseScreen;

/**
 * Client: Esc exits deposit selection mode; outline selected chests while mode is on.
 */
@EventBusSubscriber(modid = AzsCompanions.MOD_ID, value = Dist.CLIENT)
public final class DepositSelectionClientEvents {
    private DepositSelectionClientEvents() {
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof PauseScreen)) {
            return;
        }
        if (!ClientDepositSelection.isSelecting()) {
            return;
        }
        event.setCanceled(true);
        com.azscompanions.network.ModNetworking.sendToServer(new DepositExitModePacket());
        // Optimistic local hide; server sync confirms.
        ClientDepositSelection.apply(new com.azscompanions.deposit.DepositSelectionSnapshot(
                false, ClientDepositSelection.chests()));
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (!ClientDepositSelection.shouldHighlight()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        String dim = mc.level.dimension().location().toString();
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (DepositChestRef ref : ClientDepositSelection.chests()) {
            if (!ref.dimension().equals(dim)) {
                continue;
            }
            BlockPos pos = new BlockPos(ref.x(), ref.y(), ref.z());
            AABB box = new AABB(pos).inflate(0.002d);
            LevelRenderer.renderLineBox(pose, lines, box, 0.2f, 0.85f, 1.0f, 1.0f);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDepositSelection.clear();
    }
}
