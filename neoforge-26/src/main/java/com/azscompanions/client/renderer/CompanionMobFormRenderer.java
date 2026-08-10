package com.azscompanions.client.renderer;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Mob-form rendering stub for NeoForge 26.2 — full form models pending port.
 */
public final class CompanionMobFormRenderer {
    public CompanionMobFormRenderer(EntityRendererProvider.Context context) {
    }

    public void submit(
            CompanionEntity entity,
            CompanionForm form,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight
    ) {
        // No-op until form renderers are ported to SubmitNodeCollector.
    }
}
