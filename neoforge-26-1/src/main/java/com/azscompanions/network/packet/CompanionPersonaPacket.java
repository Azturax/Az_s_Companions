package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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
) implements CustomPacketPayload {
    public static final Type<CompanionPersonaPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "companion_persona"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionPersonaPacket> STREAM_CODEC =
            StreamCodec.of(CompanionPersonaPacket::write, CompanionPersonaPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, CompanionPersonaPacket p) {
        buf.writeVarInt(p.entityId);
        writeField(buf, p.whoAmI);
        writeField(buf, p.whatAmIDoing);
        writeField(buf, p.howWillIBe);
        writeField(buf, p.speechStyle);
        writeField(buf, p.relationshipToOwner);
        writeField(buf, p.quirks);
        buf.writeBoolean(p.skip);
    }

    private static CompanionPersonaPacket read(RegistryFriendlyByteBuf buf) {
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

    private static void writeField(RegistryFriendlyByteBuf buf, String value) {
        buf.writeUtf(value == null ? "" : value, CompanionPersona.MAX_LEN);
    }

    private static String readField(RegistryFriendlyByteBuf buf) {
        return buf.readUtf(CompanionPersona.MAX_LEN);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompanionPersonaPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
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
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                    companion.getChatDisplayName() + " — persona "
                            + (packet.skip() ? "skipped (defaults)" : "saved")));
        });
    }
}
