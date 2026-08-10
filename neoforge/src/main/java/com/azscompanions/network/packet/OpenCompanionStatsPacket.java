package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.ai.CompanionStatsText;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionRecruitment;
import com.azscompanions.item.CharmData;
import com.azscompanions.item.CompanionCharmItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Server → client: open Companion | Owner stats screen.
 * Synched entity fields are read on the client; this payload carries NBT-only / inventory extras.
 */
public record OpenCompanionStatsPacket(
        int entityId,
        String whoAmI,
        String whatAmIDoing,
        String howWillIBe,
        int childCount,
        int ownedCount,
        String charmStatus,
        String aiStatus
) implements CustomPacketPayload {
    public static final Type<OpenCompanionStatsPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "open_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCompanionStatsPacket> STREAM_CODEC =
            StreamCodec.of(OpenCompanionStatsPacket::write, OpenCompanionStatsPacket::read);

    public static OpenCompanionStatsPacket from(ServerPlayer player, CompanionEntity companion) {
        CompanionPersona p = companion.getPersona();
        return new OpenCompanionStatsPacket(
                companion.getId(),
                CompanionStatsText.personaSnippet(p.whoAmI()),
                CompanionStatsText.personaSnippet(p.whatAmIDoing()),
                CompanionStatsText.personaSnippet(p.howWillIBe()),
                CompanionRecruitment.countChildrenOf(player, companion.getUUID()),
                (int) CompanionRecruitment.countOwned(player),
                resolveCharmStatus(player),
                CompanionStatsText.aiStatusIfEnabled());
    }

    /** unbound | bound_active | bound_stored | none */
    public static String resolveCharmStatus(ServerPlayer player) {
        boolean anyCharm = false;
        boolean bound = false;
        boolean stored = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(stack.getItem() instanceof CompanionCharmItem)) {
                continue;
            }
            anyCharm = true;
            if (CharmData.isBound(stack)) {
                bound = true;
                if (CharmData.hasStoredCompanion(stack)) {
                    stored = true;
                }
            }
        }
        if (!anyCharm) {
            return "none";
        }
        if (stored) {
            return "bound_stored";
        }
        if (bound) {
            return "bound_active";
        }
        return "unbound";
    }

    private static void write(RegistryFriendlyByteBuf buf, OpenCompanionStatsPacket p) {
        buf.writeVarInt(p.entityId);
        int max = CompanionPersona.MAX_LEN;
        buf.writeUtf(p.whoAmI == null ? "" : p.whoAmI, max);
        buf.writeUtf(p.whatAmIDoing == null ? "" : p.whatAmIDoing, max);
        buf.writeUtf(p.howWillIBe == null ? "" : p.howWillIBe, max);
        buf.writeVarInt(p.childCount);
        buf.writeVarInt(p.ownedCount);
        buf.writeUtf(p.charmStatus == null ? "none" : p.charmStatus, 32);
        buf.writeUtf(p.aiStatus == null ? "" : p.aiStatus, CompanionStatsText.AI_SNIPPET + 8);
    }

    private static OpenCompanionStatsPacket read(RegistryFriendlyByteBuf buf) {
        int max = CompanionPersona.MAX_LEN;
        return new OpenCompanionStatsPacket(
                buf.readVarInt(),
                buf.readUtf(max),
                buf.readUtf(max),
                buf.readUtf(max),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readUtf(32),
                buf.readUtf(CompanionStatsText.AI_SNIPPET + 8));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
