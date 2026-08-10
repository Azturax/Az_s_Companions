package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** S2C: open Az admin panel with current AI snapshot + status. */
public record OpenAzAdminPacket(
        String aiJson,
        String aiStatus,
        boolean chunkLoading,
        boolean teamfight,
        String companionSummary
) implements CustomPacketPayload {
    public static final Type<OpenAzAdminPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "open_admin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAzAdminPacket> STREAM_CODEC =
            StreamCodec.of(OpenAzAdminPacket::write, OpenAzAdminPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, OpenAzAdminPacket p) {
        buf.writeUtf(p.aiJson == null ? "{}" : p.aiJson, 4096);
        buf.writeUtf(p.aiStatus == null ? "" : p.aiStatus, 512);
        buf.writeBoolean(p.chunkLoading);
        buf.writeBoolean(p.teamfight);
        buf.writeUtf(p.companionSummary == null ? "" : p.companionSummary, 1024);
    }

    private static OpenAzAdminPacket read(RegistryFriendlyByteBuf buf) {
        return new OpenAzAdminPacket(
                buf.readUtf(4096), buf.readUtf(512), buf.readBoolean(), buf.readBoolean(), buf.readUtf(1024));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
