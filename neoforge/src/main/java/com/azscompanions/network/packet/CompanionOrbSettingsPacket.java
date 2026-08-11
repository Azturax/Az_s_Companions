package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionOrbSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server update for glowing-orb customization. */
public record CompanionOrbSettingsPacket(
        int entityId,
        int colorRgb,
        int brightness,
        float floatAmplitude,
        float floatSpeed,
        float floatHeight,
        float offsetX,
        float offsetY,
        float offsetZ,
        boolean front
) implements CustomPacketPayload {
    public static final Type<CompanionOrbSettingsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "companion_orb_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionOrbSettingsPacket> STREAM_CODEC =
            StreamCodec.of(CompanionOrbSettingsPacket::write, CompanionOrbSettingsPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, CompanionOrbSettingsPacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeInt(packet.colorRgb);
        buf.writeVarInt(packet.brightness);
        buf.writeFloat(packet.floatAmplitude);
        buf.writeFloat(packet.floatSpeed);
        buf.writeFloat(packet.floatHeight);
        buf.writeFloat(packet.offsetX);
        buf.writeFloat(packet.offsetY);
        buf.writeFloat(packet.offsetZ);
        buf.writeBoolean(packet.front);
    }

    private static CompanionOrbSettingsPacket read(RegistryFriendlyByteBuf buf) {
        return new CompanionOrbSettingsPacket(
                buf.readVarInt(),
                buf.readInt(),
                buf.readVarInt(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompanionOrbSettingsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Entity entity = player.level().getEntity(packet.entityId());
            if (!(entity instanceof CompanionEntity companion)) {
                return;
            }
            if (!companion.isOwnedBy(player) && !companion.isTrusted(player)) {
                return;
            }
            if (companion.distanceTo(player) > 16.0d) {
                return;
            }
            companion.setOrbSettings(
                    CompanionOrbSettings.clampRgb(packet.colorRgb()),
                    CompanionOrbSettings.clampBrightness(packet.brightness()),
                    packet.floatAmplitude(),
                    packet.floatSpeed(),
                    packet.floatHeight(),
                    packet.offsetX(),
                    packet.offsetY(),
                    packet.offsetZ(),
                    packet.front());
        });
    }
}
