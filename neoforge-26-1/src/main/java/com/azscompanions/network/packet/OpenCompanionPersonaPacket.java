package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server → client: open Persona setup (all persona fields). Owner only. */
public record OpenCompanionPersonaPacket(
        int entityId,
        String whoAmI,
        String whatAmIDoing,
        String howWillIBe,
        String speechStyle,
        String relationshipToOwner,
        String quirks
) implements CustomPacketPayload {
    public static final Type<OpenCompanionPersonaPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "open_persona"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCompanionPersonaPacket> STREAM_CODEC =
            StreamCodec.of(OpenCompanionPersonaPacket::write, OpenCompanionPersonaPacket::read);

    public static OpenCompanionPersonaPacket fromCompanion(CompanionEntity companion) {
        CompanionPersona p = companion.getPersona();
        return new OpenCompanionPersonaPacket(
                companion.getId(),
                p.whoAmI(),
                p.whatAmIDoing(),
                p.howWillIBe(),
                p.speechStyle(),
                p.relationshipToOwner(),
                p.quirks());
    }

    private static void write(RegistryFriendlyByteBuf buf, OpenCompanionPersonaPacket p) {
        buf.writeVarInt(p.entityId);
        writeField(buf, p.whoAmI);
        writeField(buf, p.whatAmIDoing);
        writeField(buf, p.howWillIBe);
        writeField(buf, p.speechStyle);
        writeField(buf, p.relationshipToOwner);
        writeField(buf, p.quirks);
    }

    private static OpenCompanionPersonaPacket read(RegistryFriendlyByteBuf buf) {
        return new OpenCompanionPersonaPacket(
                buf.readVarInt(),
                readField(buf),
                readField(buf),
                readField(buf),
                readField(buf),
                readField(buf),
                readField(buf));
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
}
