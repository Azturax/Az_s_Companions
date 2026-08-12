package com.azscompanions.network.packet;

import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

/** Client → server: save persona fields and mark personaInitialized. */
public record CompanionPersonaPacket(
        int entityId,
        String whoAmI,
        String whatAmIDoing,
        String howWillIBe,
        String speechStyle,
        String relationshipToOwner,
        String quirks,
        boolean skip
) {

    public static void encode(CompanionPersonaPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        writeField(buf, p.whoAmI);
        writeField(buf, p.whatAmIDoing);
        writeField(buf, p.howWillIBe);
        writeField(buf, p.speechStyle);
        writeField(buf, p.relationshipToOwner);
        writeField(buf, p.quirks);
        buf.writeBoolean(p.skip);
    }

    public static CompanionPersonaPacket decode(FriendlyByteBuf buf) {
        return new CompanionPersonaPacket(
                buf.readVarInt(),
                readField(buf),
                readField(buf),
                readField(buf),
                readField(buf),
                readField(buf),
                readField(buf),
                buf.readBoolean());
    }

    private static void writeField(FriendlyByteBuf buf, String value) {
        buf.writeUtf(value == null ? "" : value, CompanionPersona.MAX_LEN);
    }

    private static String readField(FriendlyByteBuf buf) {
        return buf.readUtf(CompanionPersona.MAX_LEN);
    }

    public static void handle(CompanionPersonaPacket packet, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            Entity entity = player.level().getEntity(packet.entityId());
            if (!(entity instanceof CompanionEntity companion) || !companion.isOwnedBy(player)) {
                return;
            }
            CompanionPersona current = companion.getPersona();
            CompanionPersona next = packet.skip()
                    ? current.cleared()
                    : new CompanionPersona(
                            packet.whoAmI(),
                            packet.whatAmIDoing(),
                            packet.howWillIBe(),
                            packet.speechStyle(),
                            packet.relationshipToOwner(),
                            packet.quirks(),
                            true);
            companion.setPersona(next);
            com.azscompanions.entity.CompanionDimensionTravelSupport.rememberIdentity(player, companion);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    companion.getChatDisplayName() + " — persona "
                            + (packet.skip() ? "skipped (defaults)" : "saved")), true);
        });
        context.setPacketHandled(true);

    }
}
