package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server → client: open Persona setup (who / what / how). Owner only. */
public record OpenCompanionPersonaPacket(
        int entityId,
        String whoAmI,
        String whatAmIDoing,
        String howWillIBe
) implements CustomPacketPayload {
    public static final Type<OpenCompanionPersonaPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "open_persona"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCompanionPersonaPacket> STREAM_CODEC =
            StreamCodec.of(OpenCompanionPersonaPacket::write, OpenCompanionPersonaPacket::read);

    public static OpenCompanionPersonaPacket fromCompanion(CompanionEntity companion) {
        CompanionPersona p = companion.getPersona();
        return new OpenCompanionPersonaPacket(
                companion.getId(), p.whoAmI(), p.whatAmIDoing(), p.howWillIBe());
    }

    private static void write(RegistryFriendlyByteBuf buf, OpenCompanionPersonaPacket p) {
        buf.writeVarInt(p.entityId);
        buf.writeUtf(p.whoAmI == null ? "" : p.whoAmI, CompanionPersona.MAX_LEN);
        buf.writeUtf(p.whatAmIDoing == null ? "" : p.whatAmIDoing, CompanionPersona.MAX_LEN);
        buf.writeUtf(p.howWillIBe == null ? "" : p.howWillIBe, CompanionPersona.MAX_LEN);
    }

    private static OpenCompanionPersonaPacket read(RegistryFriendlyByteBuf buf) {
        return new OpenCompanionPersonaPacket(
                buf.readVarInt(),
                buf.readUtf(CompanionPersona.MAX_LEN),
                buf.readUtf(CompanionPersona.MAX_LEN),
                buf.readUtf(CompanionPersona.MAX_LEN));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
