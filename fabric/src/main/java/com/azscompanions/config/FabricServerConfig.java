package com.azscompanions.config;

import com.azscompanions.ai.CompanionAiConfigIO;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.entity.CompanionChunkLoading;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.CompanionInventoryPersistence;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Fabric server limits + companion AI settings.
 * Non-AI limits mirror NeoForge {@code ServerConfig} defaults.
 * AI settings live in {@code config/azscompanions-ai.json} (default provider disabled).
 * Loaded on server start (dedicated / integrated) so all companions share the host LLM.
 */
public final class FabricServerConfig {
    /** Max owned companions per player (excludes teamfight / child Bits). Default 1. */
    public static final int MAX_COMPANIONS_PER_PLAYER = 1;
    /**
     * Default max children per companion ({@code maxChildrenPerCompanion}). Living + stored.
     * CCI {@code maxChildren=}/{@code childCap=} overrides per entity (hard max 64).
     */
    public static final int MAX_CHILD_COMPANIONS_PER_LEADER = 3;
    /** If true, new team-fight sessions start enabled (normally use /az teamfight on). */
    public static final boolean TEAMFIGHT_ENABLED_BY_DEFAULT = false;
    /**
     * Interaction amount per child companion (CCI {@code amount=} ÷ this = spawn count).
     * Mirrors NeoForge {@code supportAmountPerCompanion}.
     */
    public static final int SUPPORT_AMOUNT_PER_COMPANION = 100;
    /** When true, companions defend the owner against living attackers. */
    public static final boolean ALLOW_COMBAT = true;
    /**
     * Keep companion/Bit inventory on death (no world drops). Snapshots go to charm / parent Bits.
     * Mirrors NeoForge {@code keepInventoryOnDeath}. Default true.
     */
    public static final boolean KEEP_INVENTORY_ON_DEATH =
            CompanionInventoryPersistence.DEFAULT_KEEP_INVENTORY_ON_DEATH;
    /**
     * Force-load the chunk each summoned companion/Bit occupies (entity tickets, not FTB claims).
     * Mirrors NeoForge {@code companionChunkLoading}. Default on for reliability; costs server chunks.
     */
    public static final boolean COMPANION_CHUNK_LOADING = CompanionChunkLoading.DEFAULT_ENABLED;
    /** Max companion/Bit chunk tickets per owner (parents + children). */
    public static final int MAX_FORCED_CHUNKS_PER_PLAYER =
            CompanionChunkLoading.DEFAULT_MAX_FORCED_CHUNKS_PER_PLAYER;
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

    /** Replace cached AI settings after an admin save (does not write disk by itself). */
    public static void replaceAiSettings(CompanionAiSettings next) {
        aiSettings = next == null ? new CompanionAiSettings() : next.copy();
    }
}
