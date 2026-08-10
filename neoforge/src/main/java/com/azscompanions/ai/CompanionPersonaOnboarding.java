package com.azscompanions.ai;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.network.packet.OpenCompanionPersonaPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * First-create persona onboarding (NeoForge). Ask once; charm recall never re-opens.
 * CCI whoAmI/whatAmIDoing/howWillIBe (and optional speech/relationship/quirks) sets initialized and skips this.
 */
public final class CompanionPersonaOnboarding {
    private CompanionPersonaOnboarding() {
    }

    public static void offerIfNeeded(ServerPlayer player, CompanionEntity companion) {
        if (player == null || companion == null) {
            return;
        }
        if (!CompanionPersona.shouldOfferOnboarding(
                companion.isFightSpawn(),
                companion.isChildCompanion(),
                companion.isPersonaInitialized())) {
            return;
        }
        String name = companion.getChatDisplayName();
        companion.speakLine(CompanionPersona.onboardingIntro(name));
        companion.speakLine(CompanionPersona.onboardingLineWho());
        companion.speakLine(CompanionPersona.onboardingLineWhat());
        companion.speakLine(CompanionPersona.onboardingLineHow());
        PacketDistributor.sendToPlayer(player, OpenCompanionPersonaPacket.fromCompanion(companion));
    }
}
