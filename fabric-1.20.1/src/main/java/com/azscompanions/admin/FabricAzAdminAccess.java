package com.azscompanions.admin;

import com.azscompanions.admin.AzAdminMessages;
import com.azscompanions.admin.AzAdminWhitelist;
import com.azscompanions.config.FabricAdminConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric gate for {@code /az admin} and AI config save.
 */
public final class FabricAzAdminAccess {
    private FabricAzAdminAccess() {
    }

    public static boolean isCommandEnabled() {
        return FabricAdminConfig.enableAzAdminCommand();
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
        return AzAdminWhitelist.matchesAny(
                FabricAdminConfig.adminWhitelist(),
                FabricAdminConfig.azAdminUsers(),
                player.getUUID(),
                player.getGameProfile().getName());
    }

    public static String denyMessage(ServerPlayer player) {
        if (!isCommandEnabled()) {
            return AzAdminMessages.DISABLED;
        }
        return AzAdminMessages.DENIED;
    }
}
