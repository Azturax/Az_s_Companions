package com.azscompanions.client.deposit;

import com.azscompanions.deposit.ClientDepositSelection;
import com.azscompanions.deposit.DepositChestRef;
import com.azscompanions.deposit.DepositSelectionSnapshot;
import com.azscompanions.network.FabricNetworking;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class FabricDepositClient {
    private FabricDepositClient() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!ClientDepositSelection.isSelecting()) {
                return;
            }
            if (client.screen instanceof PauseScreen) {
                client.setScreen(null);
                ClientPlayNetworking.send(new FabricNetworking.DepositExitPayload());
                ClientDepositSelection.apply(new DepositSelectionSnapshot(false, ClientDepositSelection.chests()));
            }
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (!ClientDepositSelection.shouldHighlight()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) {
                return;
            }
            String dim = mc.level.dimension().location().toString();
            PoseStack pose = context.matrixStack();
            Vec3 cam = context.camera().getPosition();
            MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
            VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);
            for (DepositChestRef ref : ClientDepositSelection.chests()) {
                if (!ref.dimension().equals(dim)) {
                    continue;
                }
                AABB box = new AABB(new BlockPos(ref.x(), ref.y(), ref.z())).inflate(0.002d);
                LevelRenderer.renderLineBox(pose, lines, box, 0.2f, 0.85f, 1.0f, 1.0f);
            }
            pose.popPose();
            buffers.endBatch(RenderType.lines());
        });
    }
}
