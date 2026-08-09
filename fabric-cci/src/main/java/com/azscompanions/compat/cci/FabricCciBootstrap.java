package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanionsFabric;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.ichun.mods.cci.api.CCIApi;
import me.ichun.mods.cci.api.IApi;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric CCI-edition bootstrap. Only compiled into {@code azscompanions-fabric-cci}.
 */
public final class FabricCciBootstrap implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(FabricCciActionPacket.TYPE, FabricCciActionPacket.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FabricCciActionPacket.TYPE, FabricCciActionPacket::handle);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("azscci")
                        .requires(source -> source.hasPermission(0))
                        .then(Commands.argument("subject", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String subject = StringArgumentType.getString(ctx, "subject");
                                    FabricCciBridge.dispatchForPlayer(player, subject, "");
                                    return 1;
                                })
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String subject = StringArgumentType.getString(ctx, "subject");
                                            String message = StringArgumentType.getString(ctx, "message");
                                            FabricCciBridge.dispatchForPlayer(player, subject, message);
                                            return 1;
                                        })))));

        IApi api = CCIApi.getApiImpl();
        boolean present = api != null && !(api.getClass().getSimpleName().equals("ApiDummy"));
        AzsCompanionsFabric.LOGGER.info(
                "CCI edition (Fabric) active — Content Creator Integration API present={}", present);
        AzsCompanionsFabric.LOGGER.info(
                "CCI bridge: IMCOutcome mixin (same subjects as NeoForge) + /azscci CommandOutcome fallback");
    }
}
