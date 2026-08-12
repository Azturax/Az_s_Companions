package com.azscompanions.client.hud;

import com.azscompanions.AzsCompanions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = AzsCompanions.MOD_ID, value = Dist.CLIENT)
public final class CompanionAiThinkingHudEvents {
    private CompanionAiThinkingHudEvents() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        CompanionAiThinkingHudOverlay.render(
                event.getGuiGraphics(),
                event.getPartialTick().getGameTimeDeltaPartialTick(true));
    }
}
