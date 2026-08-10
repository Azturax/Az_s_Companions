package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionFollowDistances;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server update for follow / personal-space / wander sliders.
 */
public record CompanionBehaviorPacket(
        int entityId,
        float followRadius,
        float personalSpace,
        float wanderRadius
) implements CustomPacketPayload {
    public static final Type<CompanionBehaviorPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "companion_behavior"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionBehaviorPacket> STREAM_CODEC =
            StreamCodec.of(CompanionBehaviorPacket::write, CompanionBehaviorPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, CompanionBehaviorPacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeFloat(packet.followRadius);
        buf.writeFloat(packet.personalSpace);
        buf.writeFloat(packet.wanderRadius);
    }

    private static CompanionBehaviorPacket read(RegistryFriendlyByteBuf buf) {
        return new CompanionBehaviorPacket(
                buf.readVarInt(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompanionBehaviorPacket packet, IPayloadContext context) {
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
            companion.setFollowRadius(CompanionFollowDistances.clampFollowRadius(packet.followRadius()));
            companion.setPersonalSpace(CompanionFollowDistances.clampPersonalSpace(packet.personalSpace()));
            companion.setWanderRadius(CompanionFollowDistances.clampWanderRadius(packet.wanderRadius()));
        });
    }
}
