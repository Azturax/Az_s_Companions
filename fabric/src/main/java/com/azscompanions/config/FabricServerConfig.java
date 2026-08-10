package com.azscompanions.config;

import com.azscompanions.ai.CompanionAiConfigIO;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.entity.CompanionFollowDistances;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Fabric server limits + companion AI settings.
 * Non-AI limits mirror NeoForge {@code ServerConfig} defaults.
 * AI settings live in {@code config/azscompanions-ai.json} (default provider disabled).
 */
public final class FabricServerConfig {
    /** Max owned companions per player (excludes teamfight / child Bits). Default 1. */
    public static final int MAX_COMPANIONS_PER_PLAYER = 1;
    /** Max CCI/cake child Bits under one leader. */
    public static final int MAX_CHILD_COMPANIONS_PER_LEADER = 6;
    /** If true, new team-fight sessions start enabled (normally use /azscompanions teamfight on). */
    public static final boolean TEAMFIGHT_ENABLED_BY_DEFAULT = false;
    /** Subs required for companion_spawn_leader. */
    public static final int TEAMFIGHT_SUB_COST_LEADER = 1;
    /** Hard cap fight spawns (leaders+children) per streamer. */
    public static final int TEAMFIGHT_MAX_FIGHT_SPAWNS = 24;
    /** When true, companions defend the owner against living attackers. */
    public static final boolean ALLOW_COMBAT = true;
    /**
     * Home-bed proximity for Follow/Wander auto behavior (blocks).
     * Matches {@link CompanionFollowDistances#HOME_BED_RADIUS}.
     */
    public static final double HOME_BED_RADIUS = CompanionFollowDistances.HOME_BED_RADIUS;
    /** Owner chat/status lines (Hello/Bye). */
    public static final boolean COMPANION_CHAT_MESSAGES = true;
    /** Auto-equip tools/weapons from backpack. Default off. */
    public static final boolean AUTO_EQUIP_TOOLS = false;

    private static volatile CompanionAiSettings aiSettings = new CompanionAiSettings();

    private FabricServerConfig() {
    }

    public static Path aiConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("azscompanions-ai.json");
    }

    public static void loadAiConfig() {
        try {
            aiSettings = CompanionAiConfigIO.loadOrCreate(aiConfigPath());
            CompanionAiRuntime.get().applySettings(aiSettings);
        } catch (Exception e) {
            aiSettings = new CompanionAiSettings();
            CompanionAiRuntime.get().applySettings(aiSettings);
            throw new RuntimeException("Failed to load " + aiConfigPath(), e);
        }
    }

    public static CompanionAiSettings aiSettings() {
        return aiSettings;
    }
}
