package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.client.screen.CompanionCreatorScreen;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: open the Fallout-style companion character creator. */
public record OpenCompanionCreatorPacket(int entityId) implements CustomPacketPayload {
    public static final Type<OpenCompanionCreatorPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "open_creator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCompanionCreatorPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OpenCompanionCreatorPacket::entityId, OpenCompanionCreatorPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenCompanionCreatorPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            Entity entity = mc.level.getEntity(packet.entityId());
            if (entity instanceof CompanionEntity companion) {
                mc.setScreen(new CompanionCreatorScreen(companion, null));
            }
        });
    }
}
