package com.azscompanions.ai;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.network.FabricNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * First-create persona onboarding (Fabric). Ask once; charm recall never re-opens.
 * CCI whoAmI/whatAmIDoing/howWillIBe sets initialized and skips this.
 */
public final class FabricCompanionPersonaOnboarding {
    private FabricCompanionPersonaOnboarding() {
    }

    public static void offerIfNeeded(ServerPlayer player, FabricCompanionEntity companion) {
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
        FabricNetworking.openPersonaSetup(player, companion);
    }
}
