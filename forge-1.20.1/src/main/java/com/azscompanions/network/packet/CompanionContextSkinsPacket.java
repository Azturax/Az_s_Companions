package com.azscompanions.network.packet;

import com.azscompanions.entity.CompanionContextSkinSupport;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionPlayerDataSupport;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client → server update for player-form activity outfit skins (sleeping / bathing / adventuring).
 */
public record CompanionContextSkinsPacket(
        int entityId,
        String sleepingSkin,
        String bathingSkin,
        String adventuringSkin
) {

    public static void encode(CompanionContextSkinsPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entityId);
        int max = CompanionContextSkinSupport.MAX_PATH_LENGTH;
        buf.writeUtf(packet.sleepingSkin == null ? "" : packet.sleepingSkin, max);
        buf.writeUtf(packet.bathingSkin == null ? "" : packet.bathingSkin, max);
        buf.writeUtf(packet.adventuringSkin == null ? "" : packet.adventuringSkin, max);
    }

    public static CompanionContextSkinsPacket decode(FriendlyByteBuf buf) {
        int max = CompanionContextSkinSupport.MAX_PATH_LENGTH;
        return new CompanionContextSkinsPacket(
                buf.readVarInt(),
                buf.readUtf(max),
                buf.readUtf(max),
                buf.readUtf(max));
    }

    public static void handle(CompanionContextSkinsPacket packet, java.util.function.Supplier<NetworkEvent.Context> ctx) {
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
            companion.setContextSkins(packet.sleepingSkin(), packet.bathingSkin(), packet.adventuringSkin());
            CompanionPlayerDataSupport.save(companion);
        });
        context.setPacketHandled(true);

    }
}
