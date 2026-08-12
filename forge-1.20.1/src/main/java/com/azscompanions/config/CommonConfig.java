package com.azscompanions.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class CommonConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_HUNGER;
    public static final ForgeConfigSpec.BooleanValue ENABLE_STAMINA;
    public static final ForgeConfigSpec.BooleanValue ENABLE_HEALING_SYSTEM;
    public static final ForgeConfigSpec.IntValue DEFAULT_TASK_RADIUS;
    public static final ForgeConfigSpec.IntValue TELEPORT_DISTANCE;
    public static final ForgeConfigSpec.IntValue PATH_STUCK_TIMEOUT_TICKS;
    public static final ForgeConfigSpec.BooleanValue TELEPORT_WHEN_STUCK;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_DIMENSIONS;
    public static final ForgeConfigSpec.BooleanValue AVOID_BREAKING_TOOLS;
    public static final ForgeConfigSpec.BooleanValue RESPECT_CLAIM_MODS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_LOOT;
    public static final ForgeConfigSpec.IntValue CONTAINER_POLL_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_BLOCKS_SCANNED_PER_TICK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

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
        TELEPORT_DISTANCE = builder.comment("Teleport near owner when farther than this (blocks). Follow starts ~10 and stops ~5 (min personal space 2). Home-bed radius is separate (default 35).")
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
                .defineListAllowEmpty("allowedDimensions", List.of(), o -> o instanceof String);
        RESPECT_CLAIM_MODS = builder.comment("Honor claim/protection mods when integrations are present.")
                .define("respectClaimMods", true);
        AVOID_BREAKING_TOOLS = builder.define("avoidBreakingTools", true);
        ENABLE_LOOT = builder.comment(
                        "Inject mod treasure loot (Companion Charm in desert pyramids, etc.). Default true.")
                .define("enableLoot", com.azscompanions.loot.CompanionLootSupport.DEFAULT_ENABLE_LOOT);
        builder.pop();

        SPEC = builder.build();
    }

    private CommonConfig() {
    }
}
