package com.azscompanions.network;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.entity.CompanionGender;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionRecruitment;
import com.azscompanions.menu.FabricRadialCommandMenu;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class FabricNetworking {
    private FabricNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(RecruitPayload.TYPE, RecruitPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RadialPayload.TYPE, RadialPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SettingsPayload.TYPE, SettingsPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RecruitPayload.TYPE, (payload, context) ->
                context.server().execute(() -> FabricCompanionRecruitment.recruit(context.player(), payload.definitionId())));

        ServerPlayNetworking.registerGlobalReceiver(RadialPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    Entity entity = player.level().getEntity(payload.entityId());
                    if (!(entity instanceof FabricCompanionEntity companion)) {
                        return;
                    }
                    try {
                        FabricRadialCommandMenu.Command command =
                                FabricRadialCommandMenu.Command.valueOf(payload.command());
                        new FabricRadialCommandMenu(0, player.getInventory(), companion.getId())
                                .runCommand(player, command);
                    } catch (IllegalArgumentException ignored) {
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    Entity entity = player.level().getEntity(payload.entityId());
                    if (!(entity instanceof FabricCompanionEntity companion) || !companion.isOwnedBy(player)) {
                        return;
                    }
                    if (companion.distanceTo(player) > 16.0d) {
                        return;
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_NAME) != 0 && !payload.name().isBlank()) {
                        String name = payload.name().trim();
                        if (name.length() > 32) {
                            name = name.substring(0, 32);
                        }
                        companion.setCustomDisplayName(name);
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_SCALE) != 0) {
                        companion.setBodyScale(payload.scale());
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_SKIN) != 0) {
                        String skin = payload.skinPath().trim();
                        if (!skin.startsWith("http:") && !skin.startsWith("https:")) {
                            companion.setSkinPath(skin);
                        }
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_SLIM) != 0) {
                        companion.setSlimArms(payload.slimArms());
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_GENDER) != 0) {
                        companion.setGender(payload.male() ? CompanionGender.MALE : CompanionGender.FEMALE);
                    }
                    if ((payload.flags() & SettingsPayload.FLAG_PROPORTIONS) != 0) {
                        companion.setBust(payload.bust());
                        companion.setWaist(payload.waist());
                        companion.setHips(payload.hips());
                        companion.setShoulders(payload.shoulders());
                        companion.setBustOffset(payload.bustOffset());
                    }
                }));
    }

    public record RecruitPayload(String definitionId) implements CustomPacketPayload {
        public static final Type<RecruitPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "recruit"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RecruitPayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RecruitPayload::definitionId, RecruitPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RadialPayload(int entityId, String command) implements CustomPacketPayload {
        public static final Type<RadialPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "radial"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RadialPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, RadialPayload::entityId,
                        ByteBufCodecs.STRING_UTF8, RadialPayload::command,
                        RadialPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SettingsPayload(
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
            int flags
    ) implements CustomPacketPayload {
        public static final int FLAG_NAME = 1;
        public static final int FLAG_SCALE = 2;
        public static final int FLAG_SKIN = 4;
        public static final int FLAG_SLIM = 8;
        public static final int FLAG_PROPORTIONS = 16;
        public static final int FLAG_GENDER = 32;

        public static final Type<SettingsPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "companion_settings"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> CODEC =
                StreamCodec.of(SettingsPayload::write, SettingsPayload::read);

        private static void write(RegistryFriendlyByteBuf buf, SettingsPayload p) {
            buf.writeVarInt(p.entityId);
            buf.writeUtf(p.name == null ? "" : p.name, 64);
            buf.writeFloat(p.scale);
            buf.writeUtf(p.skinPath == null ? "" : p.skinPath, 256);
            buf.writeBoolean(p.slimArms);
            buf.writeBoolean(p.male);
            buf.writeFloat(p.bust);
            buf.writeFloat(p.waist);
            buf.writeFloat(p.hips);
            buf.writeFloat(p.shoulders);
            buf.writeFloat(p.bustOffset);
            buf.writeVarInt(p.flags);
        }

        private static SettingsPayload read(RegistryFriendlyByteBuf buf) {
            return new SettingsPayload(
                    buf.readVarInt(), buf.readUtf(64), buf.readFloat(), buf.readUtf(256),
                    buf.readBoolean(), buf.readBoolean(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readVarInt());
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
