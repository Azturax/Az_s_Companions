package com.azscompanions.menu;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.network.packet.OpenCompanionCreatorPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side companion actions used by management UI packets (not a screen menu). */
public final class CompanionCommandActions {
    public enum Command {
        OPEN_INVENTORY,
        CUSTOMIZE,
        FOLLOW,
        STAY,
        WANDER
    }

    private CompanionCommandActions() {
    }

    public static void run(Player player, CompanionEntity companion, Command command) {
        if (companion == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!companion.isOwnedBy(player) && !companion.isTrusted(player)) {
            player.sendOverlayMessage(Component.translatable("message.azscompanions.not_owner"));
            return;
        }
        if (companion.distanceTo(player) > 64.0d) {
            return;
        }
        switch (command) {
            case OPEN_INVENTORY -> companion.openInventory(serverPlayer);
            case CUSTOMIZE -> {
                serverPlayer.closeContainer();
                PacketDistributor.sendToPlayer(serverPlayer, new OpenCompanionCreatorPacket(companion.getId()));
            }
            case FOLLOW -> {
                companion.setMode(CompanionMode.FOLLOW);
                toast(serverPlayer, companion, "message.azscompanions.mode_follow");
            }
            case STAY -> {
                companion.setMode(CompanionMode.STAY);
                toast(serverPlayer, companion, "message.azscompanions.mode_stay");
            }
            case WANDER -> {
                companion.setMode(CompanionMode.WANDER);
                toast(serverPlayer, companion, "message.azscompanions.mode_wander");
            }
        }
    }

    private static void toast(ServerPlayer player, CompanionEntity companion, String key) {
        player.sendOverlayMessage(
                Component.literal(companion.getChatDisplayName() + " — ")
                        .append(Component.translatable(key)));
    }
}
