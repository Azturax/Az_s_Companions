package com.azscompanions.admin;

import com.azscompanions.config.ServerConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** NeoForge gate for {@code /az admin} and AI config save. */
public final class NeoAzAdminAccess {
    private NeoAzAdminAccess() {
    }

    public static boolean isCommandEnabled() {
        return ServerConfig.ENABLE_AZ_ADMIN_COMMAND.get();
    }

    public static boolean mayEditServerAi(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server != null && !server.isDedicatedServer()
                && server.isSingleplayerOwner(player.getGameProfile())) {
            return true;
        }
        return mayUse(player);
    }

    public static boolean mayUse(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (!isCommandEnabled()) {
            return false;
        }
        MinecraftServer server = player.getServer();
        if (server != null && !server.isDedicatedServer()
                && server.isSingleplayerOwner(player.getGameProfile())) {
            return true;
        }
        if (player.hasPermissions(2)) {
            return true;
        }
        List<String> primary = new ArrayList<>();
        List<String> alias = new ArrayList<>();
        for (String s : ServerConfig.ADMIN_WHITELIST.get()) {
            primary.add(s);
        }
        for (String s : ServerConfig.AZ_ADMIN_USERS.get()) {
            alias.add(s);
        }
        return AzAdminWhitelist.matchesAny(
                primary, alias, player.getUUID(), player.getGameProfile().getName());
    }

    public static String denyMessage(ServerPlayer player) {
        if (!isCommandEnabled()) {
            return AzAdminMessages.DISABLED;
        }
        return AzAdminMessages.DENIED;
    }
}
