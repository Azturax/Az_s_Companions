package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Minimal stats open packet stub for NeoForge 26.2. */
public record OpenCompanionStatsPacket(
        int entityId,
        String whoAmI,
        String whatAmIDoing,
        String howWillIBe
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenCompanionStatsPacket> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "open_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCompanionStatsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenCompanionStatsPacket::entityId,
            ByteBufCodecs.STRING_UTF8, OpenCompanionStatsPacket::whoAmI,
            ByteBufCodecs.STRING_UTF8, OpenCompanionStatsPacket::whatAmIDoing,
            ByteBufCodecs.STRING_UTF8, OpenCompanionStatsPacket::howWillIBe,
            OpenCompanionStatsPacket::new
    );

    public static OpenCompanionStatsPacket from(ServerPlayer player, CompanionEntity companion) {
        var persona = companion.getPersona();
        return new OpenCompanionStatsPacket(
                companion.getId(),
                persona.whoAmI(),
                persona.whatAmIDoing(),
                persona.howWillIBe());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
