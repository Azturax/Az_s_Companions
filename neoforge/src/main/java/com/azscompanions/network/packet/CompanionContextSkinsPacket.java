package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionContextSkinSupport;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionPlayerDataSupport;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server update for player-form activity outfit skins (sleeping / bathing / adventuring).
 */
public record CompanionContextSkinsPacket(
        int entityId,
        String sleepingSkin,
        String bathingSkin,
        String adventuringSkin
) implements CustomPacketPayload {
    public static final Type<CompanionContextSkinsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "companion_context_skins"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionContextSkinsPacket> STREAM_CODEC =
            StreamCodec.of(CompanionContextSkinsPacket::write, CompanionContextSkinsPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, CompanionContextSkinsPacket packet) {
        buf.writeVarInt(packet.entityId);
        int max = CompanionContextSkinSupport.MAX_PATH_LENGTH;
        buf.writeUtf(packet.sleepingSkin == null ? "" : packet.sleepingSkin, max);
        buf.writeUtf(packet.bathingSkin == null ? "" : packet.bathingSkin, max);
        buf.writeUtf(packet.adventuringSkin == null ? "" : packet.adventuringSkin, max);
    }

    private static CompanionContextSkinsPacket read(RegistryFriendlyByteBuf buf) {
        int max = CompanionContextSkinSupport.MAX_PATH_LENGTH;
        return new CompanionContextSkinsPacket(
                buf.readVarInt(),
                buf.readUtf(max),
                buf.readUtf(max),
                buf.readUtf(max));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompanionContextSkinsPacket packet, IPayloadContext context) {
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
            companion.setContextSkins(packet.sleepingSkin(), packet.bathingSkin(), packet.adventuringSkin());
            CompanionPlayerDataSupport.save(companion);
        });
    }
}
