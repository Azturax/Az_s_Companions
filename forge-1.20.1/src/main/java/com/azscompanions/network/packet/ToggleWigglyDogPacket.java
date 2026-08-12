package com.azscompanions.network.packet;

import com.azscompanions.perk.WigglyDogPerk;
import com.azscompanions.perk.WigglyDogPerkSupport;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** C2S: toggle the UUID-gated Wiggly dog visibility. */
public record ToggleWigglyDogPacket() {
    public static void encode(ToggleWigglyDogPacket packet, FriendlyByteBuf buf) {
    }

    public static ToggleWigglyDogPacket decode(FriendlyByteBuf buf) {
        return new ToggleWigglyDogPacket();
    }


    public static void handle(ToggleWigglyDogPacket packet, java.util.function.Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (!WigglyDogPerkSupport.isEligible(player.getUUID())) {
                player.displayClientMessage(
                        Component.translatable("message.azscompanions.wiggly_dog_denied"), true);
                return;
            }
            WigglyDogPerk.toggle(player);
        });
        context.setPacketHandled(true);

    }
}
