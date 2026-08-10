package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: save persona fields and mark personaInitialized. */
public record CompanionPersonaPacket(
        int entityId,
        String whoAmI,
        String whatAmIDoing,
        String howWillIBe,
        boolean skip
) implements CustomPacketPayload {
    public static final Type<CompanionPersonaPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "companion_persona"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionPersonaPacket> STREAM_CODEC =
            StreamCodec.of(CompanionPersonaPacket::write, CompanionPersonaPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, CompanionPersonaPacket p) {
        buf.writeVarInt(p.entityId);
        buf.writeUtf(p.whoAmI == null ? "" : p.whoAmI, CompanionPersona.MAX_LEN);
        buf.writeUtf(p.whatAmIDoing == null ? "" : p.whatAmIDoing, CompanionPersona.MAX_LEN);
        buf.writeUtf(p.howWillIBe == null ? "" : p.howWillIBe, CompanionPersona.MAX_LEN);
        buf.writeBoolean(p.skip);
    }

    private static CompanionPersonaPacket read(RegistryFriendlyByteBuf buf) {
        return new CompanionPersonaPacket(
                buf.readVarInt(),
                buf.readUtf(CompanionPersona.MAX_LEN),
                buf.readUtf(CompanionPersona.MAX_LEN),
                buf.readUtf(CompanionPersona.MAX_LEN),
                buf.readBoolean());
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
                            current.speechStyle(),
                            current.relationshipToOwner(),
                            current.quirks(),
                            true);
            companion.setPersona(next);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    companion.getChatDisplayName() + " — persona "
                            + (packet.skip() ? "skipped (defaults)" : "saved")), true);
        });
    }
}
