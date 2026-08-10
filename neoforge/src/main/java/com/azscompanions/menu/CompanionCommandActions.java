package com.azscompanions.menu;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
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
        WANDER,
        /** Parent/child menu: store a world Bit on the parent (count up). */
        REMOVE_CHILD,
        /** Call next stored Bit (count down). */
        CALL_STORED_CHILD,
        DEPOSIT_SELECT,
        DEPOSIT_DONE,
        DEPOSIT_CLEAR,
        GATHER_STATUS,
        GATHER_CANCEL
    }

    private CompanionCommandActions() {
    }

    public static void run(Player player, CompanionEntity companion, Command command) {
        if (companion == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!companion.isOwnedBy(player) && !companion.isTrusted(player)) {
            player.displayClientMessage(Component.translatable("message.azscompanions.not_owner"), true);
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
            case REMOVE_CHILD -> {
                if (companion.isChildCompanion()) {
                    CompanionEntity parent = CompanionRecruitment.resolveLeader(serverPlayer, companion);
                    if (parent != null && parent.storeChild(companion)) {
                        serverPlayer.displayClientMessage(Component.translatable(
                                "message.azscompanions.child_stored"), true);
                    }
                } else if (companion.storeNextLivingChild()) {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.azscompanions.child_stored"), true);
                } else {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.azscompanions.child_none_to_store"), true);
                }
            }
            case CALL_STORED_CHILD -> {
                if (companion.isChildCompanion()) {
                    return;
                }
                CompanionEntity called = companion.callNextStoredChild(serverPlayer);
                if (called != null) {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.azscompanions.child_called", called.getChatDisplayName()), true);
                } else if (companion.getStoredChildCount() <= 0) {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.azscompanions.child_none_stored"), true);
                } else {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.azscompanions.child_limit_reached"), true);
                }
            }
            case DEPOSIT_SELECT -> {
                serverPlayer.closeContainer();
                com.azscompanions.deposit.DepositCommands.enable(serverPlayer);
            }
            case DEPOSIT_DONE -> com.azscompanions.deposit.DepositCommands.done(serverPlayer);
            case DEPOSIT_CLEAR -> com.azscompanions.deposit.DepositCommands.clear(serverPlayer);
            case GATHER_STATUS -> com.azscompanions.task.CollectMaterialAssign.status(serverPlayer, companion);
            case GATHER_CANCEL -> com.azscompanions.task.CollectMaterialAssign.cancel(serverPlayer, companion);
        }
    }

    private static void toast(ServerPlayer player, CompanionEntity companion, String key) {
        player.displayClientMessage(
                Component.literal(companion.getChatDisplayName() + " — ")
                        .append(Component.translatable(key)),
                true);
    }
}
