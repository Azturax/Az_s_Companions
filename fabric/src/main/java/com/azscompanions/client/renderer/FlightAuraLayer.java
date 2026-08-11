package com.azscompanions.client.renderer;

import com.azscompanions.entity.FabricFlyingNimbusEntity;
import com.azscompanions.entity.FlightAuraSupport;
import com.azscompanions.perk.SpecialPlayerPerks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.phys.Vec3;

/**
 * Player flight ki aura + foot-level trails (creative flight / elytra). Skips when on Jindujun.
 */
public final class FlightAuraLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public FlightAuraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        boolean flying = SpecialPlayerPerks.isOwnerActivelyFlying(player)
                || (!player.onGround() && player.getAbilities().flying);
        boolean onNimbus = player.getVehicle() instanceof FabricFlyingNimbusEntity;
        if (!FlightAuraSupport.shouldShowAura(flying, player.onGround(), player.isInWater(), onNimbus)) {
            FlightAuraRenderer.clearTrail(player.getUUID());
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean firstPersonLocal = mc.player == player && mc.options.getCameraType().isFirstPerson();
        Vec3 pos = player.getPosition(partialTick);
        Vec3 delta = player.getDeltaMovement();
        boolean ascending = delta.y > 0.05d;
        boolean moving = FlightAuraSupport.movingFastEnough(delta.x, delta.y, delta.z);
        int color = FlightAuraSupport.resolveColorRgb(-1, null);
        FlightAuraRenderer.render(
                player,
                partialTick,
                poseStack,
                buffer,
                color,
                1.0f,
                ascending,
                firstPersonLocal,
                true,
                moving || ascending,
                pos.x,
                pos.y,
                pos.z,
                player.getViewYRot(partialTick));
    }
}
