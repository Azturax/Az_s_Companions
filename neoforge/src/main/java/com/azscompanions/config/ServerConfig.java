package com.azscompanions.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Server limits / combat / teamfight. Companion AI lives in {@link AiConfig}
 * ({@code config/azscompanions-ai.toml}).
 */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_COMPANIONS_PER_PLAYER;
    public static final ModConfigSpec.IntValue MAX_COMPANIONS_PER_SERVER;
    public static final ModConfigSpec.IntValue MAX_CHILD_COMPANIONS_PER_LEADER;
    public static final ModConfigSpec.BooleanValue TEAMFIGHT_ENABLED_BY_DEFAULT;
    public static final ModConfigSpec.IntValue SUPPORT_AMOUNT_PER_COMPANION;
    public static final ModConfigSpec.BooleanValue ALLOW_COMBAT;
    public static final ModConfigSpec.BooleanValue ATTACK_NEUTRALS_ONLY_IF_HIT;
    public static final ModConfigSpec.BooleanValue ALLOW_GRIEFING;
    public static final ModConfigSpec.BooleanValue REQUIRE_OWNER_ONLINE;
    public static final ModConfigSpec.BooleanValue ALLOW_TEAM_TRUST;
    public static final ModConfigSpec.DoubleValue LOW_HEALTH_RETREAT_RATIO;
    public static final ModConfigSpec.BooleanValue LOG_TASK_EVENTS;
    public static final ModConfigSpec.BooleanValue STRICT_CHUNK_LOADING;
    public static final ModConfigSpec.BooleanValue COMPANION_CHUNK_LOADING;
    public static final ModConfigSpec.IntValue MAX_FORCED_CHUNKS_PER_PLAYER;
    public static final ModConfigSpec.DoubleValue HOME_BED_RADIUS;
    public static final ModConfigSpec.BooleanValue COMPANION_CHAT_MESSAGES;
    public static final ModConfigSpec.BooleanValue AUTO_EQUIP_TOOLS;
    public static final ModConfigSpec.BooleanValue ENABLE_AZ_ADMIN_COMMAND;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ADMIN_WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> AZ_ADMIN_USERS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("limits");
        // Designed for one girlfriend/companion per player; raise only if a server wants multiples.
        MAX_COMPANIONS_PER_PLAYER = builder
                .comment("Max owned companions per player (excludes teamfight / child Bits). Default 1.")
                .defineInRange("maxCompanionsPerPlayer", 1, 1, 32);
        MAX_COMPANIONS_PER_SERVER = builder.defineInRange("maxCompanionsPerServer", 64, 1, 512);
        MAX_CHILD_COMPANIONS_PER_LEADER = builder
                .comment("Default max children per companion (living + stored). Config key maxChildrenPerCompanion. CCI maxChildren=/childCap= overrides per entity (1–64).")
                .defineInRange("maxChildrenPerCompanion", 3, 1, 64);
        REQUIRE_OWNER_ONLINE = builder.define("requireOwnerOnline", false);
        builder.pop();

        builder.push("teamfight");
        TEAMFIGHT_ENABLED_BY_DEFAULT = builder
                .comment("If true, team fights start enabled on server boot (normally use /azscompanions teamfight on).")
                .define("enabledByDefault", false);
        SUPPORT_AMOUNT_PER_COMPANION = builder
                .comment("CCI interaction amount per child (amount= ÷ this = spawn count). Example: amount=500 with 100 → 5 children. Not a max cap.")
                .defineInRange("supportAmountPerCompanion", 100, 1, 1_000_000);
        builder.pop();

        builder.push("movement");
        HOME_BED_RADIUS = builder
                .comment("Blocks. Near home bed: Follow stays home-idle. Owner farther than this from bed → teleport+follow. Default 35.")
                .defineInRange("homeBedRadius", 35.0d, 8.0d, 128.0d);
        builder.pop();

        builder.push("behavior");
        COMPANION_CHAT_MESSAGES = builder
                .comment("Owner chat/status lines (Hello/Bye and similar). Default on but keep non-spammy.")
                .define("companionChatMessages", true);
        AUTO_EQUIP_TOOLS = builder
                .comment("Auto-equip tools/weapons from backpack into hand slots. Default off.")
                .define("autoEquipTools", false);
        builder.pop();

        builder.push("combat");
        ALLOW_COMBAT = builder.define("allowCombat", true);
        ATTACK_NEUTRALS_ONLY_IF_HIT = builder.define("attackNeutralsOnlyIfHit", true);
        LOW_HEALTH_RETREAT_RATIO = builder.defineInRange("lowHealthRetreatRatio", 0.25d, 0.05d, 0.9d);
        builder.pop();

        builder.push("permissions");
        ALLOW_GRIEFING = builder.comment("If false, companions never break player-made / claimed blocks.")
                .define("allowGriefing", false);
        ALLOW_TEAM_TRUST = builder.define("allowTeamTrust", true);
        builder.pop();

        builder.push("performance");
        STRICT_CHUNK_LOADING = builder.comment("Refuse tasks that would force-load distant chunks.")
                .define("strictChunkLoading", true);
        COMPANION_CHUNK_LOADING = builder.comment(
                        "Force-load (entity-tick) the chunk each summoned companion/Bit occupies so AI, follow, and sleep keep running when the owner walks away. Server cost scales with summoned count; not an FTB claim.")
                .define("companionChunkLoading", true);
        MAX_FORCED_CHUNKS_PER_PLAYER = builder.comment(
                        "Max companion/Bit chunk tickets per owner (parents and children both count). Raise for large teamfights.")
                .defineInRange("maxForcedChunksPerPlayer", 16, 1, 64);
        LOG_TASK_EVENTS = builder.define("logTaskEvents", true);
        builder.pop();

        builder.push("admin");
        ENABLE_AZ_ADMIN_COMMAND = builder.comment(
                        "Allow /az admin and /az ai config for ops, singleplayer/LAN host owner, or whitelist.")
                .define("enableAzAdminCommand", true);
        ADMIN_WHITELIST = builder.comment(
                        "UUID or player name entries allowed to use Az admin (in addition to ops / host).")
                .defineListAllowEmpty("adminWhitelist", List.of(), () -> "", o -> o instanceof String);
        AZ_ADMIN_USERS = builder.comment("Alias of adminWhitelist (same matching rules).")
                .defineListAllowEmpty("azAdminUsers", List.of(), () -> "", o -> o instanceof String);
        builder.pop();

        SPEC = builder.build();
    }

    private ServerConfig() {
    }
}
