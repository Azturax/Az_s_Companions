package com.azscompanions.admin;

import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.config.AiConfig;
import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.network.packet.OpenAzAdminPacket;
import com.azscompanions.network.packet.TeamFightHudPacket;
import com.azscompanions.teamfight.TeamFightSession;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-side NeoForge admin actions + AI config disk save with runtime apply. */
public final class NeoAzAdminActions {
    private NeoAzAdminActions() {
    }

    public static void openPanel(ServerPlayer player) {
        if (!NeoAzAdminAccess.mayUse(player)) {
            player.sendOverlayMessage(Component.literal(NeoAzAdminAccess.denyMessage(player)));
            return;
        }
        // Prefer live runtime so a prior admin save (incl. apiKey) is reflected without restart.
        AdminAiConfigSnapshot snap = AdminAiConfigSnapshot.fromSettings(CompanionAiRuntime.get().settings());
        TeamFightSession session = TeamFightSession.of(player.getUUID());
        PacketDistributor.sendToPlayer(player, new OpenAzAdminPacket(
                snap.toWireJson(),
                CompanionAiRuntime.get().statusLine(),
                ServerConfig.COMPANION_CHUNK_LOADING.get(),
                session.isEnabled(),
                summarizeCompanions(player)));
    }

    public static boolean saveAiConfig(ServerPlayer player, AdminAiConfigSnapshot snap) {
        if (!NeoAzAdminAccess.mayUse(player)) {
            player.sendSystemMessage(Component.literal(NeoAzAdminAccess.denyMessage(player)));
            return false;
        }
        if (snap == null) {
            player.sendSystemMessage(Component.literal(AzAdminMessages.AI_INVALID));
            return false;
        }
        String err = snap.validate();
        if (err != null) {
            player.sendSystemMessage(Component.literal(AzAdminMessages.AI_INVALID + " (" + err + ")"));
            return false;
        }
        try {
            CompanionAiSettings merged = snap.mergeInto(CompanionAiRuntime.get().settings());
            AiConfig.saveSettingsToDiskWithoutReload(merged);
            CompanionAiRuntime.get().applySettings(merged);
            player.sendSystemMessage(Component.literal(AzAdminMessages.AI_SAVED_APPLIED));
            return true;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal(AzAdminMessages.AI_SAVE_FAILED));
            return false;
        }
    }

    public static void handleAction(ServerPlayer player, String action) {
        if (!NeoAzAdminAccess.mayUse(player)) {
            player.sendSystemMessage(Component.literal(NeoAzAdminAccess.denyMessage(player)));
            return;
        }
        if (action == null || action.isBlank()) {
            return;
        }
        switch (action.trim().toUpperCase()) {
            case "TEAMFIGHT_ON" -> {
                TeamFightSession session = TeamFightSession.of(player.getUUID());
                session.setEnabled(true);
                PacketDistributor.sendToPlayer(player, new TeamFightHudPacket(session.snapshot().encode()));
                player.sendSystemMessage(Component.literal("Team fight ON"));
            }
            case "TEAMFIGHT_OFF" -> {
                TeamFightSession session = TeamFightSession.of(player.getUUID());
                session.setEnabled(false);
                PacketDistributor.sendToPlayer(player, new TeamFightHudPacket(session.snapshot().encode()));
                player.sendOverlayMessage(Component.literal("Team fight OFF"));
            }
            case "AI_STATUS" -> player.sendOverlayMessage(
                    Component.literal(CompanionAiRuntime.get().statusLine()));
            case "LIST_COMPANIONS" -> player.sendSystemMessage(
                    Component.literal(summarizeCompanions(player)));
            case "DISMISS_OWNED" -> dismissOwned(player);
            case "CHUNK_NOTE" -> player.sendSystemMessage(Component.literal(
                    "companionChunkLoading=" + ServerConfig.COMPANION_CHUNK_LOADING.get()
                            + " (entity tickets; not FTB claims). Edit server config + restart to change."));
            case "PERSONA_CLEAR_NEAREST" -> personaClearNearest(player);
            case "SHOW_ARMOR_NEAREST" -> showArmorNearest(player, true);
            case "HIDE_ARMOR_NEAREST" -> showArmorNearest(player, false);
            case "BEHAVIOR_RESET_NEAREST" -> behaviorResetNearest(player);
            default -> player.sendOverlayMessage(Component.literal("Unknown admin action: " + action));
        }
    }

    private static void dismissOwned(ServerPlayer player) {
        int removed = 0;
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            List<CompanionEntity> doomed = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                if (e instanceof CompanionEntity c && c.isOwnedBy(player) && !c.isFightSpawn()) {
                    doomed.add(c);
                }
            }
            for (CompanionEntity c : doomed) {
                c.discard();
                removed++;
            }
        }
        player.sendSystemMessage(Component.literal("Dismissed " + removed + " owned companion(s)."));
    }

    private static void personaClearNearest(ServerPlayer player) {
        CompanionEntity c = nearestOwned(player);
        if (c == null) {
            player.sendSystemMessage(Component.literal("No owned companion nearby"));
            return;
        }
        c.setPersona(c.getPersona().cleared());
        player.sendSystemMessage(Component.literal(c.getChatDisplayName() + " — persona cleared"));
    }

    private static void showArmorNearest(ServerPlayer player, boolean show) {
        CompanionEntity c = nearestOwned(player);
        if (c == null) {
            player.sendOverlayMessage(Component.literal("No owned companion nearby"));
            return;
        }
        c.setArmorVisible(show);
        player.sendSystemMessage(Component.literal(
                c.getChatDisplayName() + " — armor " + (show ? "shown" : "hidden")));
    }

    private static void behaviorResetNearest(ServerPlayer player) {
        CompanionEntity c = nearestOwned(player);
        if (c == null) {
            player.sendOverlayMessage(Component.literal("No owned companion nearby"));
            return;
        }
        c.setFollowRadius(CompanionFollowDistances.DEFAULT_FOLLOW_RADIUS);
        c.setPersonalSpace(CompanionFollowDistances.DEFAULT_PERSONAL_SPACE);
        c.setWanderRadius(CompanionFollowDistances.DEFAULT_WANDER_RADIUS);
        c.setMode(CompanionMode.FOLLOW);
        player.sendSystemMessage(Component.literal(c.getChatDisplayName() + " — behavior reset"));
    }

    private static CompanionEntity nearestOwned(ServerPlayer player) {
        CompanionEntity best = null;
        double bestDist = 48.0d;
        for (Entity e : player.level().getAllEntities()) {
            if (e instanceof CompanionEntity c && c.isOwnedBy(player)) {
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
        if (admin.level().getServer() == null) {
            return "Companions: (no server)";
        }
        for (ServerLevel level : admin.level().getServer().getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof CompanionEntity c) {
                    UUID owner = c.getOwnerUuid();
                    if (owner == null) {
                        continue;
                    }
                    total++;
                    byOwner.merge(owner, 1, Integer::sum);
                    ServerPlayer op = admin.level().getServer().getPlayerList().getPlayer(owner);
                    names.putIfAbsent(owner, op != null ? op.getGameProfile().name() : owner.toString().substring(0, 8));
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
