package com.koncompanions.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class CommonConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_HUNGER;
    public static final ModConfigSpec.BooleanValue ENABLE_STAMINA;
    public static final ModConfigSpec.BooleanValue ENABLE_HEALING_SYSTEM;
    public static final ModConfigSpec.IntValue DEFAULT_TASK_RADIUS;
    public static final ModConfigSpec.IntValue TELEPORT_DISTANCE;
    public static final ModConfigSpec.IntValue PATH_STUCK_TIMEOUT_TICKS;
    public static final ModConfigSpec.BooleanValue TELEPORT_WHEN_STUCK;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALLOWED_DIMENSIONS;
    public static final ModConfigSpec.BooleanValue AVOID_BREAKING_TOOLS;
    public static final ModConfigSpec.BooleanValue RESPECT_CLAIM_MODS;
    public static final ModConfigSpec.IntValue CONTAINER_POLL_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue MAX_BLOCKS_SCANNED_PER_TICK;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("survival");
        ENABLE_HUNGER = builder.comment("Optional hunger for companions (disabled by default).")
                .define("enableHunger", false);
        ENABLE_STAMINA = builder.comment("Optional stamina system (disabled by default).")
                .define("enableStamina", false);
        ENABLE_HEALING_SYSTEM = builder.comment("Allow companions to auto-consume permitted food when hurt.")
                .define("enableHealingSystem", true);
        builder.pop();

        builder.push("tasks");
        DEFAULT_TASK_RADIUS = builder.comment("Default gather/farm/mine radius.")
                .defineInRange("defaultTaskRadius", 16, 4, 64);
        TELEPORT_DISTANCE = builder.comment("Teleport to owner when farther than this (blocks).")
                .defineInRange("teleportDistance", 48, 16, 256);
        PATH_STUCK_TIMEOUT_TICKS = builder.comment("Ticks without progress before stuck recovery.")
                .defineInRange("pathStuckTimeoutTicks", 100, 20, 1200);
        TELEPORT_WHEN_STUCK = builder.define("teleportWhenStuck", true);
        MAX_BLOCKS_SCANNED_PER_TICK = builder.comment("Performance limit for block scans.")
                .defineInRange("maxBlocksScannedPerTick", 64, 8, 512);
        CONTAINER_POLL_COOLDOWN_TICKS = builder.defineInRange("containerPollCooldownTicks", 20, 1, 200);
        builder.pop();

        builder.push("world");
        ALLOWED_DIMENSIONS = builder.comment("Empty = all dimensions allowed.")
                .defineListAllowEmpty("allowedDimensions", List.of(), () -> "", o -> o instanceof String);
        RESPECT_CLAIM_MODS = builder.comment("Honor claim/protection mods when integrations are present.")
                .define("respectClaimMods", true);
        AVOID_BREAKING_TOOLS = builder.define("avoidBreakingTools", true);
        builder.pop();

        SPEC = builder.build();
    }

    private CommonConfig() {
    }
}
