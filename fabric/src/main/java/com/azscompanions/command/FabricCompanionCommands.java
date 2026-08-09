package com.azscompanions.command;

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
        );
    }

    private static FabricCompanionEntity nearestOwned(ServerPlayer player) {
        return player.level().getEntitiesOfClass(FabricCompanionEntity.class, player.getBoundingBox().inflate(32),
                        c -> c.isOwnedBy(player))
                .stream()
                .findFirst()
                .orElse(null);
    }
}
