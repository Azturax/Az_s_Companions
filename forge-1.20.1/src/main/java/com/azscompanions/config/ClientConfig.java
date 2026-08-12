package com.azscompanions.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_VOICE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SUBTITLES;
    public static final ForgeConfigSpec.DoubleValue VOICE_VOLUME;
    public static final ForgeConfigSpec.BooleanValue SHOW_TASK_PARTICLES;
    public static final ForgeConfigSpec.BooleanValue SHOW_3D_PREVIEW_IN_SELECTION;
    public static final ForgeConfigSpec.BooleanValue SHOW_ON_MINIMAP;
    public static final ForgeConfigSpec.BooleanValue SHOW_CHILDREN_ON_MAP;
    public static final ForgeConfigSpec.BooleanValue SHOW_NAME_ON_MAP;
    public static final ForgeConfigSpec.BooleanValue SHOW_OWNER_ON_MAP;
    public static final ForgeConfigSpec.IntValue MAP_ICON_COLOR;
    public static final ForgeConfigSpec.BooleanValue TRANSLUCENT_PLAYER_SKINS;
    public static final ForgeConfigSpec.BooleanValue SYNC_MOB_FORM_UUID;
    public static final ForgeConfigSpec.BooleanValue DYNAMIC_LIGHTS_COMPAT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("voice");
        ENABLE_VOICE = builder.comment("Play companion Minecraft sound events for dialogue categories.")
                .define("enableVoice", true);
        ENABLE_SUBTITLES = builder.comment("Show dialogue lines on the action bar.")
                .define("enableSubtitles", true);
        VOICE_VOLUME = builder.defineInRange("voiceVolume", 0.8d, 0.0d, 1.0d);
        builder.pop();

        builder.push("ui");
        SHOW_TASK_PARTICLES = builder.define("showTaskParticles", true);
        SHOW_3D_PREVIEW_IN_SELECTION = builder.define("show3dPreviewInSelection", true);
        builder.pop();

        builder.push("map");
        SHOW_ON_MINIMAP = builder.comment(
                        "Show companions on map entity radars (JourneyMap honors this; Xaero shows LivingEntity by default).")
                .define("showOnMinimap", true);
        SHOW_CHILDREN_ON_MAP = builder.comment("Show child Bits / fight children on map radars that honor this toggle.")
                .define("showChildrenOnMap", true);
        SHOW_NAME_ON_MAP = builder.comment("Prefer companion display name on JourneyMap radar labels.")
                .define("showNameOnMap", true);
        SHOW_OWNER_ON_MAP = builder.comment("Add owner hint to JourneyMap companion tooltips.")
                .define("showOwnerOnMap", true);
        MAP_ICON_COLOR = builder.comment("ARGB tint for JourneyMap companion dots/labels (e.g. -1495284 = 0xFFE91E8C).")
                .defineInRange("mapIconColor", 0xFFE91E8C, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.pop();

        builder.push("fancyAnim");
        TRANSLUCENT_PLAYER_SKINS = builder.comment(
                        "Allow translucent player-form skins when EMF/ETF is installed (ETF emissives / skin alpha / animated frames). Without EMF/ETF, companions always use cutout so skins stay visible.")
                .define("translucentPlayerSkins", true);
        SYNC_MOB_FORM_UUID = builder.comment(
                        "Mob-form proxies share the companion UUID so Fresh Animations / ETF random & emissive variants stay stable on vanilla CEM paths.")
                .define("syncMobFormUuid", true);
        builder.pop();

        builder.push("dynamicLights");
        DYNAMIC_LIGHTS_COMPAT = builder.comment(
                        "Soft-compat for LambDynamicLights / RyoamicLights / similar. Companions expose held torches via LivingEntity hand slots; enables optional legacy API registration when those mods are present.")
                .define("dynamicLightsCompat", true);
        builder.pop();

        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
