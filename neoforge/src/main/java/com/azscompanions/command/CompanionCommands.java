package com.azscompanions.command;

import com.azscompanions.ai.CompanionAiAsk;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionRecruitment;
import com.azscompanions.entity.CompanionRegistry;
import com.azscompanions.menu.CompanionSelectionMenu;
import com.azscompanions.network.packet.TeamFightHudPacket;
import com.azscompanions.teamfight.TeamFightSession;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class CompanionCommands {
    private CompanionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("azscompanions")
                .then(Commands.literal("select")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.openMenu(new CompanionSelectionMenu.Provider());
                            return 1;
                        }))
                .then(Commands.literal("recruit")
                        .executes(ctx -> recruit(ctx.getSource(), CompanionRegistry.KON_ID.toString()))
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> recruit(ctx.getSource(), StringArgumentType.getString(ctx, "id")))))
                .then(Commands.literal("home")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            CompanionEntity nearest = nearestOwned(player);
                            if (nearest == null) {
                                ctx.getSource().sendFailure(Component.literal("No owned companion nearby"));
                                return 0;
                            }
                            nearest.setHomePos(player.blockPosition());
                            ctx.getSource().sendSuccess(() -> Component.literal("Home set"), true);
                            return 1;
                        }))
                .then(Commands.literal("rename")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    CompanionEntity nearest = nearestOwned(player);
                                    if (nearest == null) {
                                        ctx.getSource().sendFailure(Component.literal("No owned companion nearby"));
                                        return 0;
                                    }
                                    String name = StringArgumentType.getString(ctx, "name").trim();
                                    if (name.isEmpty() || name.length() > 32) {
                                        ctx.getSource().sendFailure(Component.literal("Name must be 1–32 characters"));
                                        return 0;
                                    }
                                    nearest.setCustomDisplayName(name);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Renamed companion to " + name), true);
                                    return 1;
                                })))
                .then(Commands.literal("customize")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            CompanionEntity nearest = nearestOwned(player);
                            if (nearest == null) {
                                ctx.getSource().sendFailure(Component.literal("No owned companion nearby"));
                                return 0;
                            }
                            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                                    player,
                                    new com.azscompanions.network.packet.OpenCompanionCreatorPacket(nearest.getId()));
                            return 1;
                        }))
                .then(Commands.literal("ask")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    CompanionEntity nearest = nearestOwned(player);
                                    if (nearest == null) {
                                        ctx.getSource().sendFailure(Component.literal("No owned companion nearby"));
                                        return 0;
                                    }
                                    return CompanionAiAsk.ask(player, nearest, StringArgumentType.getString(ctx, "message"));
                                })))
                .then(Commands.literal("ai")
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal(CompanionAiAsk.status()), false);
                                    return 1;
                                })))
                .then(Commands.literal("teamfight")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("on").executes(ctx -> teamfight(ctx.getSource(), true)))
                        .then(Commands.literal("off").executes(ctx -> teamfight(ctx.getSource(), false)))
                        .then(Commands.literal("status").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            TeamFightSession session = TeamFightSession.of(player.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Team fight: " + (session.isEnabled() ? "ON" : "OFF")
                                            + " | HUD " + (session.isHudVisible() ? "shown" : "hidden")
                                            + " | CCI: teamfight_enable / teamfight_disable"), false);
                            return 1;
                        }))
                )
        );
    }

    private static int teamfight(CommandSourceStack source, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        TeamFightSession session = TeamFightSession.of(player.getUUID());
        session.setEnabled(enabled);
        PacketDistributor.sendToPlayer(player, new TeamFightHudPacket(session.snapshot().encode()));
        player.displayClientMessage(Component.translatable(
                enabled ? "message.azscompanions.teamfight_on" : "message.azscompanions.teamfight_off"), true);
        source.sendSuccess(() -> Component.literal("Team fight " + (enabled ? "ON" : "OFF")), true);
        return 1;
    }

    private static int recruit(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (CompanionRecruitment.recruit(player, id) != null) {
            source.sendSuccess(() -> Component.literal("Recruited " + id), true);
            return 1;
        }
        source.sendFailure(Component.translatable("message.azscompanions.limit_reached"));
        return 0;
    }

    private static CompanionEntity nearestOwned(ServerPlayer player) {
        return player.level().getEntitiesOfClass(CompanionEntity.class, player.getBoundingBox().inflate(32),
                        c -> c.isOwnedBy(player))
                .stream()
                .findFirst()
                .orElse(null);
    }
}
