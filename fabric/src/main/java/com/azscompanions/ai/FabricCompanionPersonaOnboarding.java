package com.azscompanions.ai;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.network.FabricNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * First-create persona onboarding (Fabric). Opens the persona GUI once; charm recall never re-opens.
 * CCI whoAmI/whatAmIDoing/howWillIBe sets initialized and skips this.
 * No chat spam — the GUI fields are enough.
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
        FabricNetworking.openPersonaSetup(player, companion);
    }
}
