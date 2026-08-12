package com.azscompanions.client.deposit;

import com.azscompanions.AzsCompanions;
import com.azscompanions.deposit.ClientDepositSelection;
import com.azscompanions.network.packet.DepositExitModePacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.gui.screens.PauseScreen;

/** Client lifecycle for entering and leaving deposit selection mode. */
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
        ClientPacketDistributor.sendToServer(new DepositExitModePacket());
        // Optimistic local hide; server sync confirms.
        ClientDepositSelection.apply(new com.azscompanions.deposit.DepositSelectionSnapshot(
                false, ClientDepositSelection.chests()));
    }

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientDepositSelection.clear();
    }
}
