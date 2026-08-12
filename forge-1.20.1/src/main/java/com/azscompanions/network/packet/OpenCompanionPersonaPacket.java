package com.azscompanions.network.packet;

import net.minecraftforge.network.NetworkEvent;

import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.network.FriendlyByteBuf;

/** Server → client: open Persona setup (all persona fields). Owner only. */
public record OpenCompanionPersonaPacket(
        int entityId,
        String whoAmI,
        String whatAmIDoing,
        String howWillIBe,
        String speechStyle,
        String relationshipToOwner,
        String quirks
) {

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

    public static void encode(OpenCompanionPersonaPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.entityId);
        writeField(buf, p.whoAmI);
        writeField(buf, p.whatAmIDoing);
        writeField(buf, p.howWillIBe);
        writeField(buf, p.speechStyle);
        writeField(buf, p.relationshipToOwner);
        writeField(buf, p.quirks);
    }

    public static OpenCompanionPersonaPacket decode(FriendlyByteBuf buf) {
        return new OpenCompanionPersonaPacket(
                buf.readVarInt(),
                readField(buf),
                readField(buf),
                readField(buf),
                readField(buf),
                readField(buf),
                readField(buf));
    }

    private static void writeField(FriendlyByteBuf buf, String value) {
        buf.writeUtf(value == null ? "" : value, CompanionPersona.MAX_LEN);
    }

    private static String readField(FriendlyByteBuf buf) {
        return buf.readUtf(CompanionPersona.MAX_LEN);
    }
}
