package com.azscompanions.command;

import com.azscompanions.ai.CompanionAiAsk;
import com.azscompanions.ai.CompanionAskResolve;
import com.azscompanions.ai.CompanionPersonaOnboarding;
import com.azscompanions.deposit.DepositCommands;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionRecruitment;
import com.azscompanions.entity.CompanionRegistry;
import com.azscompanions.menu.CompanionSelectionMenu;
import com.azscompanions.network.packet.OpenCompanionPersonaPacket;
import com.azscompanions.network.packet.OpenCompanionStatsPacket;
import com.azscompanions.network.packet.TeamFightHudPacket;
import com.azscompanions.teamfight.TeamFightSession;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * NeoForge commands. Primary root {@code /az}; {@code /azscompanions} kept as alias.
 * Ask UX: {@code /ask &lt;msg&gt;} and {@code /az ask [Name] &lt;msg&gt;} — owner-scoped only.
 */
public final class CompanionCommands {
    public static final double ASK_RANGE = 128.0d;

    private CompanionCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = buildRoot();
        LiteralCommandNode<CommandSourceStack> az = dispatcher.register(root);
        dispatcher.register(Commands.literal("azscompanions").redirect(az));

        dispatcher.register(Commands.literal("ask")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> askGreedy(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "message")))));

        DepositCommands.register(dispatcher);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        return Commands.literal("az")
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
                .then(CciSummonCommand.buildBranch())
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
                            PacketDistributor.sendToPlayer(
                                    player,
                                    new com.azscompanions.network.packet.OpenCompanionCreatorPacket(nearest.getId()));
                            return 1;
                        }))
                .then(Commands.literal("ask")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> askGreedy(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "message")))))
                .then(Commands.literal("ai")
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ctx.getSource().sendSuccess(() -> Component.literal(CompanionAiAsk.status()), false);
                                    return 1;
                                }))
                        .then(Commands.literal("config")
                                .executes(ctx -> {
                                    com.azscompanions.admin.NeoAzAdminActions.openPanel(
                                            ctx.getSource().getPlayerOrException());
                                    return 1;
                                })))
                .then(Commands.literal("admin")
                        .executes(ctx -> {
                            com.azscompanions.admin.NeoAzAdminActions.openPanel(
                                    ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("stats")
                        .executes(ctx -> openStats(ctx.getSource().getPlayerOrException(), null))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> openStats(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("gather")
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    CompanionEntity c = nearestOwned(player);
                                    if (c == null) {
                                        ctx.getSource().sendFailure(Component.literal("No owned companion nearby"));
                                        return 0;
                                    }
                                    return com.azscompanions.task.CollectMaterialAssign.status(player, c);
                                }))
                        .then(Commands.literal("cancel")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    CompanionEntity c = nearestOwned(player);
                                    if (c == null) {
                                        ctx.getSource().sendFailure(Component.literal("No owned companion nearby"));
                                        return 0;
                                    }
                                    return com.azscompanions.task.CollectMaterialAssign.cancel(player, c);
                                }))
                        .then(Commands.argument("item", StringArgumentType.string())
                                .then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 1_000_000))
                                        .executes(ctx -> gatherAssign(ctx, "nearest"))
                                        .then(Commands.argument("deposit", StringArgumentType.word())
                                                .executes(ctx -> gatherAssign(ctx,
                                                        StringArgumentType.getString(ctx, "deposit")))))))
                .then(DepositCommands.buildBranch())
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
                );
    }

    private static int gatherAssign(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx,
                                    String deposit) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompanionEntity c = nearestOwned(player);
        if (c == null) {
            ctx.getSource().sendFailure(Component.literal("No owned companion nearby"));
            return 0;
        }
        String item = StringArgumentType.getString(ctx, "item");
        int count = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "count");
        return com.azscompanions.task.CollectMaterialAssign.assign(player, c, item, count, deposit);
    }

    public static int askGreedy(ServerPlayer player, String greedy) {
        CompanionAskResolve.AskTarget target = CompanionAskResolve.resolveGreedyAsk(greedy,
                name -> CompanionAiAsk.ownsCompanionNamed(player, name, ASK_RANGE));
        if (!target.isValid()) {
            player.displayClientMessage(Component.literal("Usage: /ask <message> or /az ask [Name] <message>"), false);
            return 0;
        }
        CompanionEntity companion;
        if (target.kind() == CompanionAskResolve.AskTarget.Kind.NAMED) {
            companion = CompanionAiAsk.findOwnedByName(player, target.companionName(), ASK_RANGE);
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
        return CompanionAiAsk.ask(player, companion, target.message());
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
        CompanionEntity created = CompanionRecruitment.recruit(player, id);
        if (created != null) {
            CompanionPersonaOnboarding.offerIfNeeded(player, created);
            source.sendSuccess(() -> Component.literal("Recruited " + id), true);
            return 1;
        }
        source.sendFailure(Component.translatable("message.azscompanions.limit_reached"));
        return 0;
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

    private static CompanionEntity resolvePersonaTarget(ServerPlayer player, String nameOrNull) {
        if (nameOrNull == null || nameOrNull.isBlank() || nameOrNull.equalsIgnoreCase("nearest")) {
            return nearestOwned(player);
        }
        return CompanionAiAsk.findOwnedByName(player, nameOrNull, ASK_RANGE);
    }

    private static int personaShow(ServerPlayer player, String nameOrNull) {
        CompanionEntity companion = resolvePersonaTarget(player, nameOrNull);
        if (companion == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return 0;
        }
        player.displayClientMessage(Component.literal(companion.getPersona().formatSummary(companion.getChatDisplayName())), false);
        return 1;
    }

    private static int personaClear(ServerPlayer player, String nameOrNull) {
        CompanionEntity companion = resolvePersonaTarget(player, nameOrNull);
        if (companion == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return 0;
        }
        companion.setPersona(companion.getPersona().cleared());
        player.displayClientMessage(Component.literal(companion.getChatDisplayName() + " — persona cleared (still initialized)"), false);
        return 1;
    }

    private static int personaEdit(ServerPlayer player, String nameOrNull) {
        CompanionEntity companion = resolvePersonaTarget(player, nameOrNull);
        if (companion == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return 0;
        }
        PacketDistributor.sendToPlayer(player, OpenCompanionPersonaPacket.fromCompanion(companion));
        return 1;
    }

    private static int personaSet(ServerPlayer player, String nameOrNull, String field, String text) {
        CompanionEntity companion = resolvePersonaTarget(player, nameOrNull);
        if (companion == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return 0;
        }
        var before = companion.getPersona();
        var after = before.withField(field, text);
        if (after.equals(before) && field != null
                && !java.util.Set.of("who", "whoami", "what", "whatamidoing", "how", "howwillibe",
                "speech", "speechstyle", "relationship", "relationshiptoowner", "quirks", "quirk")
                .contains(field.trim().toLowerCase(java.util.Locale.ROOT))) {
            player.displayClientMessage(Component.literal(
                    "Unknown field. Use who|what|how|speech|relationship|quirks"), false);
            return 0;
        }
        companion.setPersona(after);
        player.displayClientMessage(Component.literal(companion.getChatDisplayName() + " — persona " + field + " set"), false);
        return 1;
    }

    private static int openStats(ServerPlayer player, String nameOrNull) {
        CompanionEntity companion = resolvePersonaTarget(player, nameOrNull);
        if (companion == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return 0;
        }
        PacketDistributor.sendToPlayer(player, OpenCompanionStatsPacket.from(player, companion));
        return 1;
    }

    private static CompanionEntity nearestOwned(ServerPlayer player) {
        return CompanionAiAsk.findNearestOwned(player, 32.0d);
    }
}
