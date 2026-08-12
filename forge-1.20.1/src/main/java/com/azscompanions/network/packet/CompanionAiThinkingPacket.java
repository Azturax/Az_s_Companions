package com.azscompanions.network.packet;

import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C: companion AI request started / finished for the top-right thinking HUD
 * (gear + Thinking… + progress bar).
 */
public record CompanionAiThinkingPacket(
        boolean active,
        String companionName,
        int timeoutSeconds,
        float progress
) {
    public static void encode(CompanionAiThinkingPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.active());
        buf.writeUtf(packet.companionName() == null ? "" : packet.companionName(), 8192);
        buf.writeVarInt(packet.timeoutSeconds());
        buf.writeFloat(packet.progress());
    }

    public static CompanionAiThinkingPacket decode(FriendlyByteBuf buf) {
        return new CompanionAiThinkingPacket(buf.readBoolean(), buf.readUtf(8192), buf.readVarInt(), buf.readFloat());
    }


    public static CompanionAiThinkingPacket start(String companionName, int timeoutSeconds) {
        return new CompanionAiThinkingPacket(true,
                companionName == null ? "" : companionName, timeoutSeconds, -1f);
    }

    public static CompanionAiThinkingPacket stop() {
        return new CompanionAiThinkingPacket(false, "", 0, -1f);
    }
}
