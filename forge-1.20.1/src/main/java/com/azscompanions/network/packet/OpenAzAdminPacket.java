package com.azscompanions.network.packet;

import net.minecraftforge.network.NetworkEvent;

import com.azscompanions.admin.AdminAiConfigSnapshot;
import net.minecraft.network.FriendlyByteBuf;

/** S2C: open Az admin panel with current AI snapshot + status. */
public record OpenAzAdminPacket(
        String aiJson,
        String aiStatus,
        boolean chunkLoading,
        boolean teamfight,
        String companionSummary
) {

    public static void encode(OpenAzAdminPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.aiJson == null ? "{}" : p.aiJson, AdminAiConfigSnapshot.MAX_WIRE_JSON);
        buf.writeUtf(p.aiStatus == null ? "" : p.aiStatus, 512);
        buf.writeBoolean(p.chunkLoading);
        buf.writeBoolean(p.teamfight);
        buf.writeUtf(p.companionSummary == null ? "" : p.companionSummary, 1024);
    }

    public static OpenAzAdminPacket decode(FriendlyByteBuf buf) {
        return new OpenAzAdminPacket(
                buf.readUtf(AdminAiConfigSnapshot.MAX_WIRE_JSON), buf.readUtf(512),
                buf.readBoolean(), buf.readBoolean(), buf.readUtf(1024));
    }
}
