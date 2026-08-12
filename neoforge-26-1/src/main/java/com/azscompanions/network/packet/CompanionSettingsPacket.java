package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionGender;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client → server update for appearance fields (owner/trusted, distance-gated).
 */
public record CompanionSettingsPacket(
        int entityId,
        String name,
        float scale,
        String skinPath,
        boolean slimArms,
        boolean male,
        float bust,
        float waist,
        float hips,
        float shoulders,
        float bustOffset,
        String form,
        boolean showNameTag,
        boolean showArmor,
        String formVariant,
        int flags
) implements CustomPacketPayload {
    public static final int FLAG_NAME = 1;
    public static final int FLAG_SCALE = 2;
    public static final int FLAG_SKIN = 4;
    public static final int FLAG_SLIM = 8;
    public static final int FLAG_PROPORTIONS = 16;
    public static final int FLAG_GENDER = 32;
    public static final int FLAG_FORM = 64;
    public static final int FLAG_SHOW_NAME = 128;
    public static final int FLAG_SHOW_ARMOR = 256;
    public static final int FLAG_FORM_VARIANT = 512;

    public static final Type<CompanionSettingsPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "companion_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionSettingsPacket> STREAM_CODEC =
            StreamCodec.of(CompanionSettingsPacket::write, CompanionSettingsPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, CompanionSettingsPacket packet) {
        buf.writeVarInt(packet.entityId);
        buf.writeUtf(packet.name == null ? "" : packet.name, 64);
        buf.writeFloat(packet.scale);
        buf.writeUtf(packet.skinPath == null ? "" : packet.skinPath, 256);
        buf.writeBoolean(packet.slimArms);
        buf.writeBoolean(packet.male);
        buf.writeFloat(packet.bust);
        buf.writeFloat(packet.waist);
        buf.writeFloat(packet.hips);
        buf.writeFloat(packet.shoulders);
        buf.writeFloat(packet.bustOffset);
        buf.writeUtf(packet.form == null ? CompanionForm.PLAYER.serializedName() : packet.form, 32);
        buf.writeBoolean(packet.showNameTag);
        buf.writeBoolean(packet.showArmor);
        buf.writeUtf(packet.formVariant == null ? "" : packet.formVariant, 64);
        buf.writeVarInt(packet.flags);
    }

    private static CompanionSettingsPacket read(RegistryFriendlyByteBuf buf) {
        return new CompanionSettingsPacket(
                buf.readVarInt(),
                buf.readUtf(64),
                buf.readFloat(),
                buf.readUtf(256),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readUtf(32),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(64),
                buf.readVarInt()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompanionSettingsPacket packet, IPayloadContext context) {
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
            if ((packet.flags() & FLAG_NAME) != 0) {
                String name = packet.name() == null ? "" : packet.name().trim();
                if (name.length() > 32) {
                    name = name.substring(0, 32);
                }
                if (!name.isEmpty()) {
                    companion.setCustomDisplayName(name);
                }
            }
            if ((packet.flags() & FLAG_SCALE) != 0) {
                companion.setBodyScale(packet.scale());
            }
            if ((packet.flags() & FLAG_SKIN) != 0) {
                String skin = packet.skinPath() == null ? "" : packet.skinPath().trim();
                if (skin.length() > 256) {
                    skin = skin.substring(0, 256);
                }
                if (!skin.startsWith("http:") && !skin.startsWith("https:") && !skin.startsWith("local:")) {
                    companion.setSkinPath(skin);
                }
            }
            if ((packet.flags() & FLAG_SLIM) != 0) {
                companion.setSlimArms(packet.slimArms());
            }
            if ((packet.flags() & FLAG_GENDER) != 0) {
                companion.setGender(packet.male() ? CompanionGender.MALE : CompanionGender.FEMALE);
            }
            if ((packet.flags() & FLAG_PROPORTIONS) != 0) {
                companion.setBust(packet.bust());
                companion.setWaist(packet.waist());
                companion.setHips(packet.hips());
                companion.setShoulders(packet.shoulders());
                companion.setBustOffset(packet.bustOffset());
            }
            if ((packet.flags() & FLAG_FORM) != 0) {
                companion.setForm(CompanionForm.byName(packet.form()));
            }
            if ((packet.flags() & FLAG_FORM_VARIANT) != 0) {
                companion.setFormVariant(packet.formVariant());
            }
            if ((packet.flags() & FLAG_SHOW_NAME) != 0) {
                companion.setNameTagVisible(packet.showNameTag());
            }
            if ((packet.flags() & FLAG_SHOW_ARMOR) != 0) {
                companion.setArmorVisible(packet.showArmor());
            }
            if ((packet.flags() & FLAG_FORM) != 0 && (packet.flags() & FLAG_NAME) != 0) {
                com.azscompanions.ai.CompanionPersonaOnboarding.offerIfNeeded(player, companion);
            }
        });
    }
}
