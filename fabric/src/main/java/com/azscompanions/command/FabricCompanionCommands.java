package com.azscompanions.command;

import com.azscompanions.ai.CompanionAskResolve;
import com.azscompanions.ai.FabricCompanionAiAsk;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionRecruitment;
import com.azscompanions.entity.FabricCompanionRegistry;
import com.azscompanions.menu.FabricCompanionSelectionMenu;
import com.azscompanions.network.FabricNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Fabric commands. Primary root {@code /az}; {@code /azscompanions} kept as alias.
 * Ask UX: {@code /ask &lt;msg&gt;} and {@code /az ask [Name] &lt;msg&gt;} — owner-scoped only.
 */
public final class FabricCompanionCommands {
    public static final double ASK_RANGE = 128.0d;

    private FabricCompanionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = buildRoot();
        LiteralCommandNode<CommandSourceStack> az = dispatcher.register(root);
        dispatcher.register(Commands.literal("azscompanions").redirect(az));

        dispatcher.register(Commands.literal("ask")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> askGreedy(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "message")))));

        com.azscompanions.deposit.FabricDepositCommands.register(dispatcher);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        return Commands.literal("az")
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
                            FabricCompanionEntity created = FabricCompanionRecruitment.recruitEntity(
                                    player, FabricCompanionRegistry.KON_ID.toString());
                            if (created != null) {
                                com.azscompanions.ai.FabricCompanionPersonaOnboarding.offerIfNeeded(player, created);
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
                                    FabricCompanionEntity created = FabricCompanionRecruitment.recruitEntity(player, id);
                                    if (created != null) {
                                        com.azscompanions.ai.FabricCompanionPersonaOnboarding.offerIfNeeded(player, created);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Recruited " + id), true);
                                        return 1;
                                    }
                                    ctx.getSource().sendFailure(Component.translatable("message.azscompanions.limit_reached"));
                                    return 0;
                                })))
                .then(FabricCciSummonCommand.buildBranch())
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
                                .executes(ctx -> askGreedy(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "message")))))
                .then(Commands.literal("ai")
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal(FabricCompanionAiAsk.status()), false);
                                    return 1;
                                }))
                        .then(Commands.literal("config")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    com.azscompanions.admin.FabricAzAdminActions.openPanel(player);
                                    return 1;
                                })))
                .then(Commands.literal("stats")
                        .executes(ctx -> openStats(ctx.getSource().getPlayerOrException(), null))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> openStats(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("admin")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            com.azscompanions.admin.FabricAzAdminActions.openPanel(player);
                            return 1;
                        }))
                .then(com.azscompanions.deposit.FabricDepositCommands.buildBranch())
                .then(buildPersonaBranch())
                .then(Commands.literal("wiggly")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            if (!com.azscompanions.perk.WigglyDogPerkSupport.isEligible(player.getUUID())) {
                                ctx.getSource().sendFailure(Component.translatable(
                                        "message.azscompanions.wiggly_dog_denied"));
                                return 0;
                            }
                            com.azscompanions.perk.WigglyDogPerk.toggle(player);
                            return 1;
                        }))
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
                );
    }

    /**
     * Owner-scoped ask: {@code /az ask Kon hi} targets the player's own Kon;
     * {@code /az ask hi} / {@code /ask hi} targets nearest owned companion.
     */
    public static int askGreedy(ServerPlayer player, String greedy) {
        CompanionAskResolve.AskTarget target = CompanionAskResolve.resolveGreedyAsk(greedy,
                name -> FabricCompanionAiAsk.ownsCompanionNamed(player, name, ASK_RANGE));
        if (!target.isValid()) {
            player.displayClientMessage(Component.literal("Usage: /ask <message> or /az ask [Name] <message>"), false);
            return 0;
        }
        FabricCompanionEntity companion;
        if (target.kind() == CompanionAskResolve.AskTarget.Kind.NAMED) {
            companion = FabricCompanionAiAsk.findOwnedByName(player, target.companionName(), ASK_RANGE);
            if (companion == null) {
                player.displayClientMessage(Component.literal(
                        "No owned companion named \"" + target.companionName() + "\" nearby"), false);
                return 0;
            }
        } else {
            companion = nearestOwned(player);
            if (companion == null) {
                player.displayClientMessage(Component.literal("No owned companion nearby"), false);
                return 0;
            }
        }
        return FabricCompanionAiAsk.ask(player, companion, target.message());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildPersonaBranch() {
        return Commands.literal("persona")
                .executes(ctx -> personaShow(ctx.getSource().getPlayerOrException(), null))
                .then(Commands.literal("nearest")
                        .executes(ctx -> personaShow(ctx.getSource().getPlayerOrException(), null)))
                .then(Commands.literal("clear")
                        .executes(ctx -> personaClear(ctx.getSource().getPlayerOrException(), null)))
                .then(Commands.literal("edit")
                        .executes(ctx -> personaEdit(ctx.getSource().getPlayerOrException(), null)))
                .then(Commands.literal("set")
                        .then(Commands.argument("field", StringArgumentType.word())
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> personaSet(
                                                ctx.getSource().getPlayerOrException(),
                                                null,
                                                StringArgumentType.getString(ctx, "field"),
                                                StringArgumentType.getString(ctx, "text"))))))
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> personaShow(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "name")))
                        .then(Commands.literal("clear")
                                .executes(ctx -> personaClear(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "name"))))
                        .then(Commands.literal("edit")
                                .executes(ctx -> personaEdit(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "name"))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("field", StringArgumentType.word())
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                                .executes(ctx -> personaSet(
                                                        ctx.getSource().getPlayerOrException(),
                                                        StringArgumentType.getString(ctx, "name"),
                                                        StringArgumentType.getString(ctx, "field"),
                                                        StringArgumentType.getString(ctx, "text")))))));
    }

    private static FabricCompanionEntity resolvePersonaTarget(ServerPlayer player, String nameOrNull) {
        if (nameOrNull == null || nameOrNull.isBlank() || nameOrNull.equalsIgnoreCase("nearest")) {
            return nearestOwned(player);
        }
        return FabricCompanionAiAsk.findOwnedByName(player, nameOrNull, ASK_RANGE);
    }

    private static int personaShow(ServerPlayer player, String nameOrNull) {
        FabricCompanionEntity companion = resolvePersonaTarget(player, nameOrNull);
        if (companion == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return 0;
        }
        player.displayClientMessage(Component.literal(companion.getPersona().formatSummary(companion.getChatDisplayName())), false);
        return 1;
    }

    private static int personaClear(ServerPlayer player, String nameOrNull) {
        FabricCompanionEntity companion = resolvePersonaTarget(player, nameOrNull);
        if (companion == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return 0;
        }
        companion.setPersona(companion.getPersona().cleared());
        player.displayClientMessage(Component.literal(companion.getChatDisplayName() + " — persona cleared (still initialized)"), false);
        return 1;
    }

    private static int personaEdit(ServerPlayer player, String nameOrNull) {
        FabricCompanionEntity companion = resolvePersonaTarget(player, nameOrNull);
        if (companion == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return 0;
        }
        FabricNetworking.openPersonaSetup(player, companion);
        return 1;
    }

    private static int personaSet(ServerPlayer player, String nameOrNull, String field, String text) {
        FabricCompanionEntity companion = resolvePersonaTarget(player, nameOrNull);
        if (companion == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return 0;
        }
        var before = companion.getPersona();
        var after = before.withField(field, text);
        if (after.equals(before) && !after.initialized()) {
            player.displayClientMessage(Component.literal(
                    "Unknown field. Use who|what|how|speech|relationship|quirks"), false);
            return 0;
        }
        companion.setPersona(after);
        player.displayClientMessage(Component.literal(companion.getChatDisplayName() + " — persona " + field + " set"), false);
        return 1;
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

    private static int openStats(ServerPlayer player, String nameOrNull) {
        FabricCompanionEntity companion = resolvePersonaTarget(player, nameOrNull);
        if (companion == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return 0;
        }
        FabricNetworking.openStats(player, companion);
        return 1;
    }

    private static FabricCompanionEntity nearestOwned(ServerPlayer player) {
        return FabricCompanionAiAsk.findNearestOwned(player, 32.0d);
    }
}
