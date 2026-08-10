package com.azscompanions.ai;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionForm;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * NeoForge helper: ask nearby owned companion via configured LLM / MCP provider.
 * Replies are text dialogue for every {@link CompanionForm} (player, animal, hostile).
 */
public final class CompanionAiAsk {
    private CompanionAiAsk() {
    }

    public static int ask(ServerPlayer player, CompanionEntity companion, String message) {
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        if (!runtime.isEnabled()) {
            player.displayClientMessage(Component.literal(
                    "Companion AI is disabled. Edit config/azscompanions-ai.toml (provider)."), false);
            return 0;
        }
        CompanionChatContext ctx = new CompanionChatContext(
                companion.getChatDisplayName(),
                companion.getForm().serializedName(),
                player.getGameProfile().getName(),
                message,
                runtime.settings().inputLanguage()
        );
        boolean showChat = runtime.settings().enableChatMessages();
        player.displayClientMessage(Component.literal("… " + companion.getChatDisplayName() + " is thinking"), true);
        boolean accepted = runtime.requestChatAsync(ctx, (reply, error) -> {
            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }
            server.execute(() -> deliver(player, companion, reply, error, showChat));
        });
        return accepted ? 1 : 0;
    }

    private static void deliver(ServerPlayer player, CompanionEntity companion, String reply, Throwable error,
                                boolean showChat) {
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
        if (showChat) {
            companion.speakLine(clipped);
        } else {
            player.displayClientMessage(Component.literal(clipped), false);
        }
    }

    public static String status() {
        return CompanionAiRuntime.get().statusLine();
    }
}
