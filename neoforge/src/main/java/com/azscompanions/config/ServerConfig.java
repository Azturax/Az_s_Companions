package com.azscompanions.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue MAX_COMPANIONS_PER_PLAYER;
    public static final ModConfigSpec.IntValue MAX_COMPANIONS_PER_SERVER;
    public static final ModConfigSpec.BooleanValue ALLOW_COMBAT;
    public static final ModConfigSpec.BooleanValue ATTACK_NEUTRALS_ONLY_IF_HIT;
    public static final ModConfigSpec.BooleanValue ALLOW_GRIEFING;
    public static final ModConfigSpec.BooleanValue REQUIRE_OWNER_ONLINE;
    public static final ModConfigSpec.BooleanValue ALLOW_TEAM_TRUST;
    public static final ModConfigSpec.DoubleValue LOW_HEALTH_RETREAT_RATIO;
    public static final ModConfigSpec.BooleanValue LOG_TASK_EVENTS;
    public static final ModConfigSpec.BooleanValue STRICT_CHUNK_LOADING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("limits");
        // Designed for one girlfriend/companion per player; raise only if a server wants multiples.
        MAX_COMPANIONS_PER_PLAYER = builder
                .comment("Max owned companions per player. Default 1 (one companion / girlfriend).")
                .defineInRange("maxCompanionsPerPlayer", 1, 1, 32);
        MAX_COMPANIONS_PER_SERVER = builder.defineInRange("maxCompanionsPerServer", 64, 1, 512);
        REQUIRE_OWNER_ONLINE = builder.define("requireOwnerOnline", false);
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
        LOG_TASK_EVENTS = builder.define("logTaskEvents", true);
        builder.pop();

        SPEC = builder.build();
    }

    private ServerConfig() {
    }
}
