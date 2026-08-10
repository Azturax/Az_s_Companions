package com.azscompanions.ai;

import com.azscompanions.entity.FabricCompanionEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric helper: ask nearby owned companion via configured LLM / MCP provider.
 * Replies are owner chat lines for every mob form (TTS packet path is NeoForge-only for now).
 */
public final class FabricCompanionAiAsk {
    private FabricCompanionAiAsk() {
    }

    public static int ask(ServerPlayer player, FabricCompanionEntity companion, String message) {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        if (!runtime.isEnabled()) {
            player.displayClientMessage(Component.literal(
                    "Companion AI is disabled. Edit config/azscompanions-ai.json (provider)."), false);
            return 0;
        }
        CompanionChatContext ctx = new CompanionChatContext(
                companion.getChatDisplayName(),
                companion.getForm().serializedName(),
                player.getGameProfile().getName(),
                message,
                runtime.settings().inputLanguage()
        );
        player.displayClientMessage(Component.literal("… " + companion.getChatDisplayName() + " is thinking"), true);
        boolean accepted = runtime.requestChatAsync(ctx, (reply, error) -> {
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            server.execute(() -> deliver(player, companion, reply, error));
        });
        return accepted ? 1 : 0;
    }

    private static void deliver(ServerPlayer player, FabricCompanionEntity companion, String reply, Throwable error) {
        if (!player.isAlive() || companion.isRemoved()) {
            return;
        }
        if (error != null) {
            player.displayClientMessage(Component.literal("Companion AI error: " + error.getMessage()), false);
            return;
        }
        if (reply == null || reply.isBlank()) {
            player.displayClientMessage(Component.literal("Companion AI returned an empty reply."), false);
            return;
        }
        String clipped = reply.length() > 512 ? reply.substring(0, 509) + "…" : reply;
        companion.speakLine(clipped);
    }

    public static String status() {
        return CompanionAiRuntime.get().statusLine();
    }
}
