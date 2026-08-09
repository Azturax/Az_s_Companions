package com.azscompanions.voice;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.network.packet.CompanionDialoguePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server entrypoint for dialogue. Actual sound/TTS/Voicemod playback is client-side.
 */
public final class VoiceService {
    private static final VoiceService INSTANCE = new VoiceService();

    private VoiceService() {
    }

    public static VoiceService get() {
        return INSTANCE;
    }

    public void speak(CompanionEntity companion, DialogueCategory category, String line) {
        if (companion.level().isClientSide) {
            return;
        }
        String name = companion.getChatDisplayName();
        companion.setCustomName(companion.hasCustomName() ? companion.getCustomName() : Component.literal(name));
        if (companion.getOwner() instanceof ServerPlayer owner) {
            owner.displayClientMessage(Component.literal("<" + name + "> " + line), false);
            PacketDistributor.sendToPlayer(owner, new CompanionDialoguePacket(
                    companion.getId(),
                    category.name(),
                    line,
                    companion.getVoiceProfile()
            ));
        }
    }
}
