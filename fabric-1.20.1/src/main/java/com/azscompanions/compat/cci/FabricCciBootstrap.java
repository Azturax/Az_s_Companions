package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.cci.CciMessages;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.ichun.mods.cci.api.CCIApi;
import me.ichun.mods.cci.api.IApi;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric CCI soft-compat for Minecraft 1.20.1 (legacy networking).
 * Loaded only when CCI is present (see {@link FabricCciCompatModule}).
 */
public final class FabricCciBootstrap {
    private FabricCciBootstrap() {
    }

    /** Called reflectively from {@link FabricCciCompatModule}. */
    public static void bootstrap() {
        ServerPlayNetworking.registerGlobalReceiver(FabricCciActionPacket.ID, (server, player, handler, buf, responseSender) -> {
            FabricCciActionPacket packet = FabricCciActionPacket.read(buf);
            server.execute(() -> FabricCciActionPacket.handle(player, packet));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("azscci")
                        .requires(source -> source.hasPermission(0))
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> CciMsg.t(CciMessages.COMMAND_USAGE), false);
                            return 0;
                        })
                        .then(Commands.argument("subject", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String subject = StringArgumentType.getString(ctx, "subject");
                                    return runAzscci(player, subject, "");
                                })
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String subject = StringArgumentType.getString(ctx, "subject");
                                            String message = StringArgumentType.getString(ctx, "message");
                                            return runAzscci(player, subject, message);
                                        })))));

        IApi api = CCIApi.getApiImpl();
        boolean present = api != null && !(api.getClass().getSimpleName().equals("ApiDummy"));
        AzsCompanionsFabric.LOGGER.info(
                "CCI soft-compat (Fabric) active — Content Creator Integration API present={}", present);
        AzsCompanionsFabric.LOGGER.info(
                "CCI bridge: IMCOutcome mixin (same subjects as NeoForge) + /azscci CommandOutcome fallback");
    }

    private static int runAzscci(ServerPlayer player, String subject, String message) {
        FabricCciCompanionAction action = FabricCciCompanionAction.fromSubject(subject);
        if (action == null) {
            player.displayClientMessage(CciMsg.t(CciMessages.UNKNOWN_SUBJECT, subject), false);
            return 0;
        }
        FabricCciCompanionActions.applyOnServer(player, action, message == null ? "" : message);
        return 1;
    }
}
