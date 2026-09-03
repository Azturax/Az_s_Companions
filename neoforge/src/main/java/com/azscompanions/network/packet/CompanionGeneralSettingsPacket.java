package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.ChatListenMode;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionPlayerAiPrefs;
import com.azscompanions.entity.CompanionPlayerDataSupport;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server general companion settings from the charm-menu gear screen.
 * Owner/trusted only. Does not include LLM keys.
 */
public record CompanionGeneralSettingsPacket(
        int entityId,
        boolean showNameTag,
        boolean teleportEnabled,
        boolean globalTalk,
        boolean idleChat,
        String chatListen,
        boolean chunkLoading
) implements CustomPacketPayload {
    public static final Type<CompanionGeneralSettingsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "companion_general_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionGeneralSettingsPacket> STREAM_CODEC =
            StreamCodec.of(CompanionGeneralSettingsPacket::write, CompanionGeneralSettingsPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, CompanionGeneralSettingsPacket p) {
        buf.writeVarInt(p.entityId);
        buf.writeBoolean(p.showNameTag);
        buf.writeBoolean(p.teleportEnabled);
        buf.writeBoolean(p.globalTalk);
        buf.writeBoolean(p.idleChat);
        buf.writeUtf(p.chatListen == null ? "" : p.chatListen, 16);
        buf.writeBoolean(p.chunkLoading);
    }

    private static CompanionGeneralSettingsPacket read(RegistryFriendlyByteBuf buf) {
        return new CompanionGeneralSettingsPacket(
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(16),
                buf.readBoolean());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompanionGeneralSettingsPacket packet, IPayloadContext context) {
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
            companion.setNameTagVisible(packet.showNameTag());
            companion.setTeleportEnabled(packet.teleportEnabled());
            companion.setGlobalTalkEnabled(packet.globalTalk());
            companion.setIdleChatEnabled(packet.idleChat());
            companion.setChatListenMode(CompanionPlayerAiPrefs.parseChatListen(
                    packet.chatListen(), ChatListenMode.GLOBAL));
            companion.setChunkLoadingEnabled(packet.chunkLoading());
            CompanionPlayerDataSupport.save(companion);
        });
    }
}
