package com.azscompanions.admin;

import com.azscompanions.ai.CompanionAiConfigIO;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.config.FabricServerConfig;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.entity.FabricCompanionPlayerDataSupport;
import com.azscompanions.network.FabricNetworking;
import com.azscompanions.teamfight.TeamFightSession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side admin actions + AI config disk save with runtime apply.
 */
public final class FabricAzAdminActions {
    private FabricAzAdminActions() {
    }

    public static void openPanel(ServerPlayer player) {
        if (!FabricAzAdminAccess.mayUse(player)) {
            player.displayClientMessage(Component.literal(FabricAzAdminAccess.denyMessage(player)), false);
            return;
        }
        sendOpen(player, false, true);
    }

    public static void openAiConfig(ServerPlayer player) {
        if (player == null) {
            return;
        }
        sendOpen(player, true, FabricAzAdminAccess.mayEditServerAi(player));
    }

    private static void sendOpen(ServerPlayer player, boolean playerFacing, boolean canEditServerAi) {
        AdminAiConfigSnapshot snap = AdminAiConfigSnapshot.fromSettings(FabricServerConfig.aiSettings());
        String status = CompanionAiRuntime.get().statusLine();
        boolean chunkLoading = FabricServerConfig.COMPANION_CHUNK_LOADING;
        TeamFightSession session = TeamFightSession.of(player.getUUID());
        FabricNetworking.openAdminPanel(player, snap, status, chunkLoading, session.isEnabled(),
                summarizeCompanions(player), playerFacing, canEditServerAi);
    }

    public static boolean saveAiConfig(ServerPlayer player, AdminAiConfigSnapshot snap) {
        if (snap == null) {
            player.displayClientMessage(Component.literal(AzAdminMessages.AI_INVALID), false);
            return false;
        }
        boolean secrets = FabricAzAdminAccess.mayEditServerAi(player);
        String err = secrets ? snap.validate() : null;
        if (err != null) {
            player.displayClientMessage(Component.literal(AzAdminMessages.AI_INVALID + " (" + err + ")"), false);
            return false;
        }
        try {
            CompanionAiSettings merged;
            if (secrets) {
                merged = snap.mergeInto(FabricServerConfig.aiSettings());
                CompanionAiConfigIO.save(FabricServerConfig.aiConfigPath(), merged);
                FabricServerConfig.replaceAiSettings(merged);
            } else {
                merged = snap.mergePlayerFacingInto(FabricServerConfig.aiSettings());
            }
            CompanionAiRuntime.get().applySettings(merged);
            applyChatPrefsToOwned(player, snap);
            player.displayClientMessage(Component.literal(
                    secrets ? AzAdminMessages.AI_SAVED_APPLIED : AzAdminMessages.AI_PLAYER_SAVED), false);
            return true;
        } catch (Exception e) {
            player.displayClientMessage(Component.literal(AzAdminMessages.AI_SAVE_FAILED), false);
            return false;
        }
    }

    private static void applyChatPrefsToOwned(ServerPlayer player, AdminAiConfigSnapshot snap) {
        if (player.getServer() == null) {
            return;
        }
        var listen = snap.chatListen();
        boolean talk = snap.globalTalk();
        boolean idle = snap.idleChat();
        for (ServerLevel level : player.getServer().getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof FabricCompanionEntity c && c.isOwnedBy(player) && !c.isFightSpawn()) {
                    c.setChatListenMode(listen);
                    c.setGlobalTalkEnabled(talk);
                    c.setIdleChatEnabled(idle);
                    FabricCompanionPlayerDataSupport.save(c);
                }
            }
        }
    }

    public static void handleAction(ServerPlayer player, String action) {
        if (!FabricAzAdminAccess.mayUse(player)) {
            player.displayClientMessage(Component.literal(FabricAzAdminAccess.denyMessage(player)), false);
            return;
        }
        if (action == null || action.isBlank()) {
            return;
        }
        switch (action.trim().toUpperCase()) {
            case "TEAMFIGHT_ON" -> {
                TeamFightSession session = TeamFightSession.of(player.getUUID());
                session.setEnabled(true);
                FabricNetworking.sendTeamFightHud(player, session.snapshot().encode());
                player.displayClientMessage(Component.literal("Team fight ON"), true);
            }
            case "TEAMFIGHT_OFF" -> {
                TeamFightSession session = TeamFightSession.of(player.getUUID());
                session.setEnabled(false);
                FabricNetworking.sendTeamFightHud(player, session.snapshot().encode());
                player.displayClientMessage(Component.literal("Team fight OFF"), true);
            }
            case "AI_STATUS" -> player.displayClientMessage(
                    Component.literal(CompanionAiRuntime.get().statusLine()), false);
            case "LIST_COMPANIONS" -> player.displayClientMessage(
                    Component.literal(summarizeCompanions(player)), false);
            case "DISMISS_OWNED" -> dismissOwned(player);
            case "CHUNK_NOTE" -> player.displayClientMessage(Component.literal(
                    "companionChunkLoading=" + FabricServerConfig.COMPANION_CHUNK_LOADING
                            + " (entity tickets; not FTB claims). Edit server config + restart to change."), false);
            case "PERSONA_CLEAR_NEAREST" -> personaClearNearest(player);
            case "SHOW_ARMOR_NEAREST" -> showArmorNearest(player, true);
            case "HIDE_ARMOR_NEAREST" -> showArmorNearest(player, false);
            case "BEHAVIOR_RESET_NEAREST" -> behaviorResetNearest(player);
            default -> player.displayClientMessage(Component.literal("Unknown admin action: " + action), false);
        }
    }

    private static void dismissOwned(ServerPlayer player) {
        int removed = 0;
        for (ServerLevel level : player.getServer().getAllLevels()) {
            List<FabricCompanionEntity> doomed = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                if (e instanceof FabricCompanionEntity c && c.isOwnedBy(player) && !c.isFightSpawn()) {
                    doomed.add(c);
                }
            }
            for (FabricCompanionEntity c : doomed) {
                c.discard();
                removed++;
            }
        }
        player.displayClientMessage(Component.literal("Dismissed " + removed + " owned companion(s)."), false);
    }

    private static void personaClearNearest(ServerPlayer player) {
        FabricCompanionEntity c = nearestOwned(player);
        if (c == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return;
        }
        c.setPersona(c.getPersona().cleared());
        player.displayClientMessage(Component.literal(c.getChatDisplayName() + " — persona cleared"), true);
    }

    private static void showArmorNearest(ServerPlayer player, boolean show) {
        FabricCompanionEntity c = nearestOwned(player);
        if (c == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return;
        }
        c.setArmorVisible(show);
        player.displayClientMessage(Component.literal(
                c.getChatDisplayName() + " — armor " + (show ? "shown" : "hidden")), true);
    }

    private static void behaviorResetNearest(ServerPlayer player) {
        FabricCompanionEntity c = nearestOwned(player);
        if (c == null) {
            player.displayClientMessage(Component.literal("No owned companion nearby"), false);
            return;
        }
        c.setFollowRadius(CompanionFollowDistances.DEFAULT_FOLLOW_RADIUS);
        c.setPersonalSpace(CompanionFollowDistances.DEFAULT_PERSONAL_SPACE);
        c.setWanderRadius(CompanionFollowDistances.DEFAULT_WANDER_RADIUS);
        c.setMode(FabricCompanionMode.FOLLOW);
        player.displayClientMessage(Component.literal(c.getChatDisplayName() + " — behavior reset"), true);
    }

    private static FabricCompanionEntity nearestOwned(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        FabricCompanionEntity best = null;
        double bestDist = 48.0d;
        for (Entity e : level.getAllEntities()) {
            if (e instanceof FabricCompanionEntity c && c.isOwnedBy(player)) {
                double d = c.distanceTo(player);
                if (d < bestDist) {
                    bestDist = d;
                    best = c;
                }
            }
        }
        return best;
    }

    private static String summarizeCompanions(ServerPlayer admin) {
        Map<UUID, Integer> byOwner = new HashMap<>();
        Map<UUID, String> names = new HashMap<>();
        int total = 0;
        if (admin.getServer() == null) {
            return "Companions: (no server)";
        }
        for (ServerLevel level : admin.getServer().getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof FabricCompanionEntity c) {
                    UUID owner = c.getOwnerUuid();
                    if (owner == null) {
                        continue;
                    }
                    total++;
                    byOwner.merge(owner, 1, Integer::sum);
                    ServerPlayer op = admin.getServer().getPlayerList().getPlayer(owner);
                    names.putIfAbsent(owner, op != null ? op.getGameProfile().getName() : owner.toString().substring(0, 8));
                }
            }
        }
        StringBuilder sb = new StringBuilder("Companions online: ").append(total);
        byOwner.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(12)
                .forEach(e -> sb.append(" | ").append(names.getOrDefault(e.getKey(), "?"))
                        .append("=").append(e.getValue()));
        if (byOwner.size() > 12) {
            sb.append(" | …");
        }
        return sb.toString();
    }
}
