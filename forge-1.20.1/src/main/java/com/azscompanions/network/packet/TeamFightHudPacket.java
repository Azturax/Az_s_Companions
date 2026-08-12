package com.azscompanions.network.packet;

import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;

/** S2C team-fight scoreboard HUD snapshot. */
public record TeamFightHudPacket(String payload) {
    public static void encode(TeamFightHudPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.payload() == null ? "" : packet.payload(), 8192);
    }

    public static TeamFightHudPacket decode(FriendlyByteBuf buf) {
        return new TeamFightHudPacket(buf.readUtf(8192));
    }

}
