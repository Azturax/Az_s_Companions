package com.azscompanions.network.packet;

import com.azscompanions.AzsCompanions;
import com.azscompanions.perk.WigglyDogPerk;
import com.azscompanions.perk.WigglyDogPerkSupport;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S: toggle the UUID-gated Wiggly dog visibility. */
public record ToggleWigglyDogPacket() implements CustomPacketPayload {
    public static final Type<ToggleWigglyDogPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AzsCompanions.MOD_ID, "toggle_wiggly_dog"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleWigglyDogPacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleWigglyDogPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleWigglyDogPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!WigglyDogPerkSupport.isEligible(player.getUUID())) {
                player.displayClientMessage(
                        Component.translatable("message.azscompanions.wiggly_dog_denied"), true);
                return;
            }
            WigglyDogPerk.toggle(player);
        });
    }
}
