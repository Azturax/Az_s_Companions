package com.azscompanions.command;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.TaskRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class DebugCommands {
    private DebugCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kondebug")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("task")
                        .executes(ctx -> inspectTask(ctx.getSource())))
                .then(Commands.literal("path")
                        .executes(ctx -> {
                            CompanionEntity c = nearest(ctx.getSource());
                            if (c == null) {
                                return fail(ctx.getSource());
                            }
                            var nav = c.getNavigation();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Path done=" + nav.isDone() + " inProgress=" + nav.isInProgress()), false);
                            return 1;
                        }))
                .then(Commands.literal("inventory")
                        .executes(ctx -> {
                            CompanionEntity c = nearest(ctx.getSource());
                            if (c == null) {
                                return fail(ctx.getSource());
                            }
                            int filled = 0;
                            for (int i = 0; i < c.getCompanionInventory().getSlots(); i++) {
                                if (!c.getCompanionInventory().getStackInSlot(i).isEmpty()) {
                                    filled++;
                                }
                            }
                            int finalFilled = filled;
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Inventory filled slots=" + finalFilled + "/" + c.getCompanionInventory().getSlots()), false);
                            return 1;
                        }))
                .then(Commands.literal("compat")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Task types=" + TaskRegistry.factories().keySet()), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "Compat bootstrap already applied (see logs for optional modules)"), false);
                            return 1;
                        }))
                .then(Commands.literal("reset")
                        .executes(ctx -> {
                            CompanionEntity c = nearest(ctx.getSource());
                            if (c == null) {
                                return fail(ctx.getSource());
                            }
                            c.getTaskQueue().clear();
                            c.setMode(com.azscompanions.entity.CompanionMode.FOLLOW);
                            ctx.getSource().sendSuccess(() -> Component.literal("Companion reset"), true);
                            return 1;
                        }))
                .then(Commands.literal("enqueue")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(ctx -> {
                                    CompanionEntity c = nearest(ctx.getSource());
                                    if (c == null) {
                                        return fail(ctx.getSource());
                                    }
                                    String type = StringArgumentType.getString(ctx, "type");
                                    return TaskRegistry.create(type).map(task -> {
                                        c.getTaskQueue().enqueue(task);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Enqueued " + type), true);
                                        return 1;
                                    }).orElseGet(() -> {
                                        ctx.getSource().sendFailure(Component.literal("Unknown task " + type));
                                        return 0;
                                    });
                                })))
        );
    }

    private static int inspectTask(CommandSourceStack source) {
        CompanionEntity c = nearest(source);
        if (c == null) {
            return fail(source);
        }
        source.sendSuccess(() -> Component.literal(c.getTaskQueue().describeActive().orElse("no active task")), false);
        source.sendSuccess(() -> Component.literal("Queued=" + c.getTaskQueue().queued().size()), false);
        return 1;
    }

    private static CompanionEntity nearest(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            return null;
        }
        return player.level().getEntitiesOfClass(CompanionEntity.class, player.getBoundingBox().inflate(48),
                        c -> c.isOwnedBy(player) || source.hasPermission(2))
                .stream().findFirst().orElse(null);
    }

    private static int fail(CommandSourceStack source) {
        source.sendFailure(Component.literal("No companion found"));
        return 0;
    }
}
