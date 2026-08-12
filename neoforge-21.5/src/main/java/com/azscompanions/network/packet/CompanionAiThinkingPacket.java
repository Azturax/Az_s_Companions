package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: companion AI request started / finished for the top-right thinking HUD
 * (gear + Thinking… + progress bar).
 */
public record CompanionAiThinkingPacket(
        boolean active,
        String companionName,
        int timeoutSeconds,
        float progress
) implements CustomPacketPayload {
    public static final Type<CompanionAiThinkingPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "companion_ai_thinking"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionAiThinkingPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, CompanionAiThinkingPacket::active,
                    ByteBufCodecs.STRING_UTF8, CompanionAiThinkingPacket::companionName,
                    ByteBufCodecs.VAR_INT, CompanionAiThinkingPacket::timeoutSeconds,
                    ByteBufCodecs.FLOAT, CompanionAiThinkingPacket::progress,
                    CompanionAiThinkingPacket::new
            );

    public static CompanionAiThinkingPacket start(String companionName, int timeoutSeconds) {
        return new CompanionAiThinkingPacket(true,
                companionName == null ? "" : companionName, timeoutSeconds, -1f);
    }

    public static CompanionAiThinkingPacket stop() {
        return new CompanionAiThinkingPacket(false, "", 0, -1f);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
