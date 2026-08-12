package com.azscompanions.client.hud;

import com.azscompanions.AzsCompanions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = AzsCompanions.MOD_ID, value = Dist.CLIENT)
public final class TeamFightHudEvents {
    private TeamFightHudEvents() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        TeamFightHudOverlay.render(event.getGuiGraphics(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
    }
}
