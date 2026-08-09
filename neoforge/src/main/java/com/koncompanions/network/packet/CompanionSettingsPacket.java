package com.koncompanions.network.packet;

import com.koncompanions.KonCompanions;
import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionGender;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server update for name, scale, skin, slim arms, gender, and body proportions.
 */
public record CompanionSettingsPacket(
        int entityId,
        String name,
        float scale,
        String skinPath,
        boolean slimArms,
        boolean male,
        float bust,
        float waist,
        float hips,
        float shoulders,
        float bustOffset,
        int flags
) implements CustomPacketPayload {
    public static final int FLAG_NAME = 1;
    public static final int FLAG_SCALE = 2;
    public static final int FLAG_SKIN = 4;
    public static final int FLAG_SLIM = 8;
    public static final int FLAG_PROPORTIONS = 16;
    public static final int FLAG_GENDER = 32;

    public static final Type<CompanionSettingsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(KonCompanions.MOD_ID, "companion_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionSettingsPacket> STREAM_CODEC =
            StreamCodec.of(CompanionSettingsPacket::write, CompanionSettingsPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, CompanionSettingsPacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeUtf(packet.name == null ? "" : packet.name, 64);
        buf.writeFloat(packet.scale);
        buf.writeUtf(packet.skinPath == null ? "" : packet.skinPath, 256);
        buf.writeBoolean(packet.slimArms);
        buf.writeBoolean(packet.male);
        buf.writeFloat(packet.bust);
        buf.writeFloat(packet.waist);
        buf.writeFloat(packet.hips);
        buf.writeFloat(packet.shoulders);
        buf.writeFloat(packet.bustOffset);
        buf.writeVarInt(packet.flags);
    }

    private static CompanionSettingsPacket read(RegistryFriendlyByteBuf buf) {
        return new CompanionSettingsPacket(
                buf.readVarInt(),
                buf.readUtf(64),
                buf.readFloat(),
                buf.readUtf(256),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readVarInt()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompanionSettingsPacket packet, IPayloadContext context) {
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
            if ((packet.flags() & FLAG_NAME) != 0) {
                String name = packet.name() == null ? "" : packet.name().trim();
                if (name.length() > 32) {
                    name = name.substring(0, 32);
                }
                if (!name.isEmpty()) {
                    companion.setCustomDisplayName(name);
                }
            }
            if ((packet.flags() & FLAG_SCALE) != 0) {
                companion.setBodyScale(packet.scale());
            }
            if ((packet.flags() & FLAG_SKIN) != 0) {
                String skin = packet.skinPath() == null ? "" : packet.skinPath().trim();
                if (skin.length() > 256) {
                    skin = skin.substring(0, 256);
                }
                if (skin.startsWith("http:") || skin.startsWith("https:")) {
                    return;
                }
                companion.setSkinPath(skin);
            }
            if ((packet.flags() & FLAG_SLIM) != 0) {
                companion.setSlimArms(packet.slimArms());
            }
            if ((packet.flags() & FLAG_GENDER) != 0) {
                companion.setGender(packet.male() ? CompanionGender.MALE : CompanionGender.FEMALE);
            }
            if ((packet.flags() & FLAG_PROPORTIONS) != 0) {
                companion.setBust(packet.bust());
                companion.setWaist(packet.waist());
                companion.setHips(packet.hips());
                companion.setShoulders(packet.shoulders());
                companion.setBustOffset(packet.bustOffset());
            }
        });
    }
}
