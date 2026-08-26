package com.azscompanions.network.packet;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.CompanionPlayerDataSupport;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client → server update for follow / personal-space / wander sliders.
 */
public record CompanionBehaviorPacket(
        int entityId,
        float followRadius,
        float personalSpace,
        float wanderRadius
) {

    public static void encode(CompanionBehaviorPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        buf.writeFloat(packet.followRadius);
        buf.writeFloat(packet.personalSpace);
        buf.writeFloat(packet.wanderRadius);
    }

    public static CompanionBehaviorPacket decode(FriendlyByteBuf buf) {
        return new CompanionBehaviorPacket(
                buf.readVarInt(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    public static void handle(CompanionBehaviorPacket packet, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
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
            CompanionPlayerDataSupport.save(companion);
        });
        context.setPacketHandled(true);

    }
}
