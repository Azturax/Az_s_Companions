package com.azscompanions.client;

import com.azscompanions.AzsCompanions;
import com.azscompanions.client.screen.CompanionCommandScreen;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.network.packet.ToggleWigglyDogPacket;
import com.azscompanions.perk.WigglyDogPerkSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;

/** Opens the companion Command menu / toggles UUID-gated Wiggly dog. */
@EventBusSubscriber(modid = AzsCompanions.MOD_ID, value = Dist.CLIENT)
public final class CompanionCommandKeyHandler {
    private CompanionCommandKeyHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ModKeyMappings.OPEN_COMMAND_MENU.consumeClick()) {
            if (mc.gui.screen() != null || mc.player == null || mc.level == null) {
                continue;
            }
            CompanionEntity companion = findTarget(mc);
            if (companion == null) {
                mc.player.sendOverlayMessage(
                        Component.translatable("message.azscompanions.command_no_companion"));
                continue;
            }
            mc.gui.setScreen(new CompanionCommandScreen(companion, null));
        }
        while (ModKeyMappings.TOGGLE_WIGGLY_DOG.consumeClick()) {
            if (mc.gui.screen() != null || mc.player == null) {
                continue;
            }
            if (!WigglyDogPerkSupport.isEligible(mc.player.getUUID())) {
                mc.player.sendOverlayMessage(
                        Component.translatable("message.azscompanions.wiggly_dog_denied"));
                continue;
            }
            ClientPacketDistributor.sendToServer(new ToggleWigglyDogPacket());
        }
    }

    @Nullable
    private static CompanionEntity findTarget(Minecraft mc) {
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return null;
        }
        if (mc.crosshairPickEntity instanceof CompanionEntity looked
                && canCommand(looked, player)
                && looked.distanceTo(player) <= 64.0d) {
            return looked;
        }
        CompanionEntity best = null;
        double bestDist = 32.0d * 32.0d;
        for (CompanionEntity companion : mc.level.getEntitiesOfClass(
                CompanionEntity.class,
                player.getBoundingBox().inflate(32.0d),
                e -> canCommand(e, player))) {
            double dist = companion.distanceToSqr(player);
            if (dist < bestDist) {
                bestDist = dist;
                best = companion;
            }
        }
        return best;
    }

    private static boolean canCommand(CompanionEntity companion, Player player) {
        return companion.isOwnedBy(player) || companion.isTrusted(player);
    }
}
