package com.azscompanions.compat.cci;

import com.azscompanions.cci.CciCompanionParams;
import com.azscompanions.config.FabricServerConfig;
import com.azscompanions.entity.CompanionAttitude;
import com.azscompanions.entity.CompanionChildLimits;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionRecruitment;
import com.azscompanions.entity.inventory.FabricCompanionInventory;
import com.azscompanions.network.FabricNetworking;
import com.azscompanions.teamfight.BitGearTiers;
import com.azscompanions.teamfight.TeamFightChatParser;
import com.azscompanions.teamfight.TeamFightDefaults;
import com.azscompanions.teamfight.TeamFightSession;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

/** CCI-first team-fight control + leader/child spawns with bit gear tiers (Fabric). */
public final class TeamFightCciHandler {
    private TeamFightCciHandler() {
    }

    public static boolean handle(ServerPlayer player, FabricCciCompanionAction action, String message) {
        if (!action.isTeamFightControl() && action != FabricCciCompanionAction.SPAWN_CHILD) {
            return false;
        }
        CciCompanionParams params = TeamFightChatParser.parseChatOrMessage(message);
        TeamFightSession session = TeamFightSession.of(player.getUUID());
        switch (action) {
            case TEAMFIGHT_ENABLE -> setEnabled(player, session, true);
            case TEAMFIGHT_DISABLE -> setEnabled(player, session, false);
            case TEAMFIGHT_TOGGLE -> setEnabled(player, session, !session.isEnabled());
            case TEAMFIGHT_STATUS -> toast(player, "Team fight",
                    session.isEnabled() ? "ON (HUD " + (session.isHudVisible() ? "shown" : "hidden") + ")" : "OFF");
            case TEAMFIGHT_SCOREBOARD -> scoreboard(player, session, params, message);
            case TEAMFIGHT_SCORE -> score(player, session, params);
            case TEAMFIGHT_TOP -> top(player, session);
            case SPAWN_LEADER -> spawnLeader(player, session, params);
            case SPAWN_CHILD -> spawnChild(player, session, params);
            default -> {
                return false;
            }
        }
        return true;
    }

    public static void syncHud(ServerPlayer player) {
        TeamFightSession session = TeamFightSession.of(player.getUUID());
        FabricNetworking.sendTeamFightHud(player, session.snapshot().encode());
    }

    public static void setEnabled(ServerPlayer player, boolean enabled) {
        setEnabled(player, TeamFightSession.of(player.getUUID()), enabled);
    }

    private static void setEnabled(ServerPlayer player, TeamFightSession session, boolean enabled) {
        session.setEnabled(enabled);
        syncHud(player);
        player.displayClientMessage(Component.translatable(
                enabled ? "message.azscompanions.teamfight_on" : "message.azscompanions.teamfight_off"), true);
        toast(player, "Team fight", enabled ? "ENABLED" : "DISABLED");
    }

    private static void scoreboard(ServerPlayer player, TeamFightSession session, CciCompanionParams params, String raw) {
        if (!session.isEnabled()) {
            toast(player, "Scoreboard", "Enable team fights first.");
            return;
        }
        String mode = params.first("mode", "action", "raw");
        if (mode == null || mode.isBlank()) {
            mode = raw == null ? "show" : raw.trim().toLowerCase(Locale.ROOT);
        }
        mode = mode.toLowerCase(Locale.ROOT);
        if (mode.contains("hide") || "off".equals(mode)) {
            session.setHudVisible(false);
        } else if (mode.contains("reset")) {
            session.resetScores();
            session.setHudVisible(true);
        } else {
            session.setHudVisible(true);
            session.setTeams(params.getOr("team1", params.getOr("left", session.teamLeft())),
                    params.getOr("team2", params.getOr("right", session.teamRight())));
        }
        syncHud(player);
        toast(player, "Scoreboard", session.isHudVisible() ? "shown" : "hidden");
    }

    private static void score(ServerPlayer player, TeamFightSession session, CciCompanionParams params) {
        if (!session.isEnabled()) {
            toast(player, "Score", Component.translatable("message.azscompanions.teamfight_disabled").getString());
            return;
        }
        if (params.has("killer") || params.has("kill")) {
            session.recordKill(params.first("killer", "kill", "name"));
        } else {
            String team = params.teamOr(session.teamLeft());
            int points = 1;
            if (params.has("points")) {
                try {
                    points = Integer.parseInt(params.get("points").trim());
                } catch (Exception ignored) {
                }
            }
            session.addScore(team, points);
        }
        syncHud(player);
        toast(player, "Score", session.teamLeft() + "=" + session.scoreLeft()
                + " | " + session.teamRight() + "=" + session.scoreRight());
    }

    private static void top(ServerPlayer player, TeamFightSession session) {
        player.displayClientMessage(Component.literal("Top bits: " + blankDash(session.snapshot().topBits())), false);
        player.displayClientMessage(Component.literal("Top kills: " + blankDash(session.snapshot().topKills())), false);
        toast(player, "Leaderboards", BitGearTiers.priceTableText());
    }

    private static void spawnLeader(ServerPlayer player, TeamFightSession session, CciCompanionParams params) {
        if (!session.isEnabled()) {
            toast(player, "Spawn leader", Component.translatable("message.azscompanions.teamfight_disabled").getString());
            return;
        }
        int subs = params.subsOr(0);
        if (subs == 0) {
            subs = TeamFightDefaults.SUB_COST_LEADER;
        }
        int cost = FabricServerConfig.TEAMFIGHT_SUB_COST_LEADER;
        if (subs < cost) {
            toast(player, "Spawn leader", "Need ≥" + cost + " sub(s) (got " + subs + "). Use subs=.");
            return;
        }
        if (!session.canSpawnMore(FabricServerConfig.TEAMFIGHT_MAX_FIGHT_SPAWNS)) {
            toast(player, "Spawn leader", "Fight spawn cap reached.");
            return;
        }
        FabricCompanionEntity leader = FabricCompanionRecruitment.spawnFightLeader(player);
        if (leader == null) {
            toast(player, "Spawn leader", "Spawn failed.");
            return;
        }
        String team = params.teamOr(session.teamLeft());
        leader.setTeamId(team);
        leader.setAttitude(params.attitudeOr(CompanionAttitude.HOSTILE));
        leader.setForm(params.formOr(CompanionForm.ZOMBIE));
        String name = params.displayName();
        if (name != null && !name.isBlank()) {
            leader.setCustomDisplayName(name);
        }
        applyGear(leader, params, params.bitsOr(TeamFightDefaults.TIER_IRON_BITS));
        session.addFightSpawn();
        session.recordFighter(leader.getChatDisplayName(), team, 0);
        session.noteFight("Leader " + leader.getChatDisplayName() + " → " + team);
        syncHud(player);
        toast(player, leader.getChatDisplayName(), "Leader on team " + team);
    }

    private static void spawnChild(ServerPlayer player, TeamFightSession session, CciCompanionParams params) {
        int bits = params.bitsOr(0);
        // Bits path requires teamfight ON; cake-like CCI (no bits) works either way.
        if (!session.isEnabled() && bits > 0) {
            toast(player, "Spawn Bit", Component.translatable("message.azscompanions.teamfight_disabled").getString());
            return;
        }
        if (session.isEnabled() && !session.canSpawnMore(FabricServerConfig.TEAMFIGHT_MAX_FIGHT_SPAWNS)) {
            toast(player, "Spawn Bit", "Fight spawn cap reached.");
            return;
        }
        FabricCompanionEntity near = findLeader(player, params);
        if (near == null) {
            toast(player, "Spawn Bit", "No leader nearby — use companion_spawn_leader first.");
            return;
        }
        FabricCompanionEntity leader = FabricCompanionRecruitment.resolveLeader(player, near);
        if (leader == null) {
            toast(player, "Spawn Bit", "No leader available.");
            return;
        }
        int requested = params.spawnCountOr(1);
        int existing = FabricCompanionRecruitment.countChildrenOf(player, leader.getUUID());
        int remaining = CompanionChildLimits.remainingSlots(existing, FabricServerConfig.MAX_CHILD_COMPANIONS_PER_LEADER);
        int toSpawn = Math.min(requested, remaining);
        if (toSpawn <= 0) {
            toast(player, leader.getChatDisplayName(), "Child limit reached.");
            return;
        }
        String baseName = params.displayName();
        if (baseName == null || baseName.isBlank()) {
            baseName = CompanionChildLimits.DEFAULT_NAME;
        }
        int spawned = 0;
        for (int i = 0; i < toSpawn; i++) {
            FabricCompanionEntity child = FabricCompanionRecruitment.spawnChild(player, leader);
            if (child == null) {
                break;
            }
            child.setAttitude(params.attitudeOr(leader.getAttitude()));
            if (params.has("form") || params.has("mob") || params.has("species")) {
                child.setForm(params.formOr(CompanionForm.CHICKEN));
            }
            if (params.has("team") || params.has("teamid")) {
                child.setTeamId(params.teamOr(leader.getTeamId()));
            }
            child.setCustomDisplayName(toSpawn > 1 ? baseName + " " + (i + 1) : baseName);
            if (params.has("size") || params.has("scale")) {
                child.setBodyScale(params.bodyScaleOr(CompanionChildLimits.DEFAULT_BODY_SCALE));
            }
            applyGear(child, params, bits);
            if (session.isEnabled()) {
                session.addFightSpawn();
                int share = bits / Math.max(1, toSpawn);
                session.addBits(child.getTeamId(), share);
                session.recordFighter(child.getChatDisplayName(), child.getTeamId(), share);
            }
            spawned++;
        }
        if (session.isEnabled() && bits > 0) {
            session.noteFight(bits + " bits → " + spawned + " Bit(s)");
        }
        syncHud(player);
        BitGearTiers.GearLoadout gear = BitGearTiers.forBits(bits);
        toast(player, leader.getChatDisplayName(),
                "Spawned " + spawned + " Bit(s)" + (bits > 0 ? " (" + bits + " bits / " + gear.label() + ")" : ""));
    }

    private static void applyGear(FabricCompanionEntity entity, CciCompanionParams params, int bits) {
        if (params.has("mainhand") || params.has("main") || params.has("helmet")
                || params.has("chestplate") || params.has("leggings") || params.has("boots")) {
            setSlot(entity, FabricCompanionInventory.MAIN_HAND, EquipmentSlot.MAINHAND, params.first("mainhand", "main", "hand"));
            setSlot(entity, FabricCompanionInventory.OFF_HAND, EquipmentSlot.OFFHAND, params.first("offhand", "off"));
            setSlot(entity, FabricCompanionInventory.HEAD, EquipmentSlot.HEAD, params.first("helmet", "head"));
            setSlot(entity, FabricCompanionInventory.CHEST, EquipmentSlot.CHEST, params.first("chestplate", "chest"));
            setSlot(entity, FabricCompanionInventory.LEGS, EquipmentSlot.LEGS, params.first("leggings", "legs"));
            setSlot(entity, FabricCompanionInventory.FEET, EquipmentSlot.FEET, params.first("boots", "feet"));
            return;
        }
        BitGearTiers.GearLoadout gear = BitGearTiers.forBits(bits);
        if (gear.tier() <= 0) {
            return;
        }
        setSlot(entity, FabricCompanionInventory.MAIN_HAND, EquipmentSlot.MAINHAND, gear.mainhand());
        setSlot(entity, FabricCompanionInventory.HEAD, EquipmentSlot.HEAD, gear.helmet());
        setSlot(entity, FabricCompanionInventory.CHEST, EquipmentSlot.CHEST, gear.chestplate());
        setSlot(entity, FabricCompanionInventory.LEGS, EquipmentSlot.LEGS, gear.leggings());
        setSlot(entity, FabricCompanionInventory.FEET, EquipmentSlot.FEET, gear.boots());
    }

    private static void setSlot(FabricCompanionEntity entity, int invSlot, EquipmentSlot eq, String itemId) {
        if (itemId == null || itemId.isBlank() || CciCompanionParams.isClearToken(itemId)) {
            return;
        }
        Optional<ItemStack> stack = parseItem(itemId);
        if (stack.isEmpty()) {
            return;
        }
        entity.getCompanionInventory().setItem(invSlot, stack.get());
        entity.setItemSlot(eq, stack.get().copy());
    }

    private static Optional<ItemStack> parseItem(String itemId) {
        String id = itemId.trim();
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        ResourceLocation loc = ResourceLocation.tryParse(id.toLowerCase(Locale.ROOT));
        if (loc == null || !BuiltInRegistries.ITEM.containsKey(loc)) {
            return Optional.empty();
        }
        Item item = BuiltInRegistries.ITEM.get(loc);
        return Optional.of(new ItemStack(item));
    }

    private static FabricCompanionEntity findLeader(ServerPlayer player, CciCompanionParams params) {
        String team = params.teamOr("");
        var list = player.serverLevel().getEntitiesOfClass(FabricCompanionEntity.class,
                player.getBoundingBox().inflate(96),
                c -> c.isAlive() && c.isOwnedBy(player) && !c.isChildCompanion());
        if (!team.isBlank()) {
            Optional<FabricCompanionEntity> match = list.stream()
                    .filter(c -> team.equalsIgnoreCase(c.getTeamId()))
                    .min(Comparator.comparingDouble(c -> c.distanceToSqr(player)));
            if (match.isPresent()) {
                return match.get();
            }
        }
        return list.stream().min(Comparator.comparingDouble(c -> c.distanceToSqr(player))).orElse(null);
    }

    private static String blankDash(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }

    private static void toast(ServerPlayer player, String title, String body) {
        player.displayClientMessage(Component.literal(title + " — " + body), true);
    }
}
