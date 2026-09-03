package com.azscompanions.menu;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.entity.CompanionPlayerDataSupport;
import com.azscompanions.entity.CompanionRecruitment;
import com.azscompanions.network.packet.OpenCompanionCreatorPacket;
import com.azscompanions.network.packet.OpenCompanionStatsPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side companion actions used by management UI packets (not a screen menu). */
public final class CompanionCommandActions {
    public enum Command {
        OPEN_INVENTORY,
        OPEN_STATS,
        CUSTOMIZE,
        FOLLOW,
        STAY,
        SIT,
        WANDER,
        /** Parent/child menu: store a world Bit on the parent (count up). */
        REMOVE_CHILD,
        /** Call next stored Bit (count down). */
        CALL_STORED_CHILD
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
            case OPEN_STATS -> {
                serverPlayer.closeContainer();
                PacketDistributor.sendToPlayer(
                        serverPlayer, OpenCompanionStatsPacket.from(serverPlayer, companion));
            }
            case CUSTOMIZE -> {
                serverPlayer.closeContainer();
                PacketDistributor.sendToPlayer(serverPlayer, new OpenCompanionCreatorPacket(companion.getId()));
            }
            case FOLLOW -> {
                companion.setMode(CompanionMode.FOLLOW);
                CompanionPlayerDataSupport.save(companion);
                toast(serverPlayer, companion, "message.azscompanions.mode_follow");
            }
            case STAY -> {
                companion.setMode(CompanionMode.STAY);
                CompanionPlayerDataSupport.save(companion);
                toast(serverPlayer, companion, "message.azscompanions.mode_stay");
            }
            case SIT -> {
                companion.setMode(CompanionMode.SIT);
                CompanionPlayerDataSupport.save(companion);
                toast(serverPlayer, companion, "message.azscompanions.mode_sit");
            }
            case WANDER -> {
                companion.setMode(CompanionMode.WANDER);
                CompanionPlayerDataSupport.save(companion);
                toast(serverPlayer, companion, "message.azscompanions.mode_wander");
            }
            case REMOVE_CHILD -> {
                if (companion.isChildCompanion()) {
                    CompanionEntity parent = CompanionRecruitment.resolveLeader(serverPlayer, companion);
                    if (parent != null && parent.storeChild(companion)) {
                        serverPlayer.sendOverlayMessage(Component.translatable(
                                "message.azscompanions.child_stored"));
                    }
                } else if (companion.storeNextLivingChild()) {
                    serverPlayer.sendOverlayMessage(Component.translatable(
                            "message.azscompanions.child_stored"));
                } else {
                    serverPlayer.sendOverlayMessage(Component.translatable(
                            "message.azscompanions.child_none_to_store"));
                }
            }
            case CALL_STORED_CHILD -> {
                if (companion.isChildCompanion()) {
                    return;
                }
                CompanionEntity called = companion.callNextStoredChild(serverPlayer);
                if (called != null) {
                    serverPlayer.sendOverlayMessage(Component.translatable(
                            "message.azscompanions.child_called", called.getChatDisplayName()));
                } else if (companion.getStoredChildCount() <= 0) {
                    serverPlayer.sendOverlayMessage(Component.translatable(
                            "message.azscompanions.child_none_stored"));
                } else {
                    serverPlayer.sendOverlayMessage(Component.translatable(
                            "message.azscompanions.child_limit_reached"));
                }
            }
        }
    }

    private static void toast(ServerPlayer player, CompanionEntity companion, String key) {
        player.sendOverlayMessage(
                Component.literal(companion.getChatDisplayName() + " — ")
                        .append(Component.translatable(key)));
    }
}
