package com.azscompanions.client.hud;

import com.azscompanions.AzsCompanions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = AzsCompanions.MOD_ID, value = Dist.CLIENT)
public final class TeamFightHudEvents {
    private TeamFightHudEvents() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        TeamFightHudOverlay.render(event.getGuiGraphics(), event.getPartialTick());
    }
}
