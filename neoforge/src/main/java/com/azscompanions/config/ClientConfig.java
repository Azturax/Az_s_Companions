package com.azscompanions.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLE_VOICE;
    public static final ModConfigSpec.BooleanValue ENABLE_SUBTITLES;
    public static final ModConfigSpec.BooleanValue ENABLE_VOICEMOD_BRIDGE;
    public static final ModConfigSpec.DoubleValue VOICE_VOLUME;
    public static final ModConfigSpec.BooleanValue SHOW_TASK_PARTICLES;
    public static final ModConfigSpec.BooleanValue SHOW_3D_PREVIEW_IN_SELECTION;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("voice");
        ENABLE_VOICE = builder.comment("Play companion voice / sound events locally.")
                .define("enableVoice", true);
        ENABLE_SUBTITLES = builder.define("enableSubtitles", true);
        ENABLE_VOICEMOD_BRIDGE = builder.comment("Optional external Voicemod bridge (never required).")
                .define("enableVoicemodBridge", false);
        VOICE_VOLUME = builder.defineInRange("voiceVolume", 0.8d, 0.0d, 1.0d);
        builder.pop();

        builder.push("ui");
        SHOW_TASK_PARTICLES = builder.define("showTaskParticles", true);
        SHOW_3D_PREVIEW_IN_SELECTION = builder.define("show3dPreviewInSelection", true);
        builder.pop();

        SPEC = builder.build();
    }

    private ClientConfig() {
    }
}
