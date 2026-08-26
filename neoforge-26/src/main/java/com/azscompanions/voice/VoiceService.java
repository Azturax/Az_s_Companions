package com.azscompanions.voice;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.network.packet.CompanionDialoguePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server entrypoint for dialogue text (and optional client sound cues via packet).
 * Companion AI is text-first: owner chat lines work for every companion form.
 */
public final class VoiceService {
    private static final VoiceService INSTANCE = new VoiceService();

    private VoiceService() {
    }

    public static VoiceService get() {
        return INSTANCE;
    }

    public void speak(CompanionEntity companion, DialogueCategory category, String line) {
        if (companion.level().isClientSide()) {
            return;
        }
        String name = companion.getChatDisplayName();
        companion.setCustomName(companion.hasCustomName() ? companion.getCustomName() : Component.literal(name));
        if (companion.getOwner() instanceof ServerPlayer owner) {
            // Overlay + sound only. AI idle/react uses speakLine (chat). Sending both was spam.
            PacketDistributor.sendToPlayer(owner, new CompanionDialoguePacket(
                    companion.getId(),
                    category.name(),
                    line,
                    companion.getVoiceProfile()
            ));
        }
    }
}
