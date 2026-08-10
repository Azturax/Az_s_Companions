package com.azscompanions.command;

import com.azscompanions.ai.FabricCompanionAiAsk;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionRecruitment;
import com.azscompanions.entity.FabricCompanionRegistry;
import com.azscompanions.menu.FabricCompanionSelectionMenu;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class FabricCompanionCommands {
    private FabricCompanionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("azscompanions")
                .then(Commands.literal("select")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            player.openMenu(new MenuProvider() {
                                @Override
                                public Component getDisplayName() {
                                    return Component.translatable("screen.azscompanions.selection");
                                }

                                @Override
                                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                                    return new FabricCompanionSelectionMenu(id, inv);
                                }
                            });
                            return 1;
                        }))
                .then(Commands.literal("recruit")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            boolean ok = FabricCompanionRecruitment.recruit(player, FabricCompanionRegistry.KON_ID.toString());
                            if (ok) {
                                ctx.getSource().sendSuccess(() -> Component.literal("Recruited Kon"), true);
                                return 1;
                            }
                            ctx.getSource().sendFailure(Component.translatable("message.azscompanions.limit_reached"));
                            return 0;
                        })
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "id");
                                    boolean ok = FabricCompanionRecruitment.recruit(player, id);
                                    if (ok) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("Recruited " + id), true);
                                        return 1;
                                    }
                                    ctx.getSource().sendFailure(Component.translatable("message.azscompanions.limit_reached"));
                                    return 0;
                                })))
                .then(Commands.literal("rename")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    FabricCompanionEntity nearest = nearestOwned(player);
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
                                    ctx.getSource().sendSuccess(() -> Component.literal("Renamed to " + name), true);
                                    return 1;
                                })))
                .then(Commands.literal("size")
                        .then(Commands.argument("scale", FloatArgumentType.floatArg(
                                        FabricCompanionEntity.MIN_BODY_SCALE, FabricCompanionEntity.MAX_BODY_SCALE))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    FabricCompanionEntity nearest = nearestOwned(player);
                                    if (nearest == null) {
                                        ctx.getSource().sendFailure(Component.literal("No owned companion nearby"));
                                        return 0;
                                    }
                                    float scale = FloatArgumentType.getFloat(ctx, "scale");
                                    nearest.setBodyScale(scale);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Size set to " + scale), true);
                                    return 1;
                                })))
                .then(Commands.literal("skin")
                        .then(Commands.argument("path", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    FabricCompanionEntity nearest = nearestOwned(player);
                                    if (nearest == null) {
                                        ctx.getSource().sendFailure(Component.literal("No owned companion nearby"));
                                        return 0;
                                    }
                                    String path = StringArgumentType.getString(ctx, "path").trim();
                                    if (path.startsWith("http:") || path.startsWith("https:")) {
                                        ctx.getSource().sendFailure(Component.literal("URL skins are disabled"));
                                        return 0;
                                    }
                                    nearest.setSkinPath(path);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Skin set"), true);
                                    return 1;
                                })))
                .then(Commands.literal("inventory")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            FabricCompanionEntity nearest = nearestOwned(player);
                            if (nearest == null) {
                                ctx.getSource().sendFailure(Component.literal("No owned companion nearby"));
                                return 0;
                            }
                            nearest.openInventory(player);
                            return 1;
                        }))
                .then(Commands.literal("ask")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    FabricCompanionEntity nearest = nearestOwned(player);
                                    if (nearest == null) {
                                        ctx.getSource().sendFailure(Component.literal("No owned companion nearby"));
                                        return 0;
                                    }
                                    return FabricCompanionAiAsk.ask(player, nearest,
                                            StringArgumentType.getString(ctx, "message"));
                                })))
                .then(Commands.literal("ai")
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal(FabricCompanionAiAsk.status()), false);
                                    return 1;
                                })))
                .then(Commands.literal("teamfight")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("on")
                                .executes(ctx -> teamfight(ctx.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> teamfight(ctx.getSource(), false)))
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    var session = com.azscompanions.teamfight.TeamFightSession.of(player.getUUID());
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "Team fight: " + (session.isEnabled() ? "ON" : "OFF")
                                                    + " | HUD " + (session.isHudVisible() ? "shown" : "hidden")
                                                    + " | Also: /azscci teamfight_enable (CCI edition)"), false);
                                    return 1;
                                }))
                )
        );
    }

    private static int teamfight(CommandSourceStack source, boolean enabled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var session = com.azscompanions.teamfight.TeamFightSession.of(player.getUUID());
        session.setEnabled(enabled);
        com.azscompanions.network.FabricNetworking.sendTeamFightHud(player, session.snapshot().encode());
        player.displayClientMessage(Component.translatable(
                enabled ? "message.azscompanions.teamfight_on" : "message.azscompanions.teamfight_off"), true);
        source.sendSuccess(() -> Component.literal("Team fight " + (enabled ? "ON" : "OFF")), true);
        return 1;
    }

    private static FabricCompanionEntity nearestOwned(ServerPlayer player) {
        return player.level().getEntitiesOfClass(FabricCompanionEntity.class, player.getBoundingBox().inflate(32),
                        c -> c.isOwnedBy(player))
                .stream()
                .findFirst()
                .orElse(null);
    }
}
