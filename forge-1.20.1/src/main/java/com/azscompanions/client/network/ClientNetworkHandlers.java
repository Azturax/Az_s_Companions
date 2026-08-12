package com.azscompanions.client.network;

import com.azscompanions.ai.ClientCompanionAiHud;
import com.azscompanions.client.screen.AzAdminScreen;
import com.azscompanions.client.screen.CompanionCreatorScreen;
import com.azscompanions.client.screen.CompanionMenuScreen;
import com.azscompanions.client.screen.CompanionPersonaScreen;
import com.azscompanions.client.screen.CompanionStatsScreen;
import com.azscompanions.client.voice.ClientVoiceController;
import com.azscompanions.deposit.ClientDepositSelection;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.network.packet.CompanionAiJoinOfferPacket;
import com.azscompanions.network.packet.CompanionAiThinkingPacket;
import com.azscompanions.network.packet.CompanionDialoguePacket;
import com.azscompanions.network.packet.DepositSelectionSyncPacket;
import com.azscompanions.network.packet.OpenAzAdminPacket;
import com.azscompanions.network.packet.OpenCompanionCreatorPacket;
import com.azscompanions.network.packet.OpenCompanionMenuPacket;
import com.azscompanions.network.packet.OpenCompanionPersonaPacket;
import com.azscompanions.network.packet.OpenCompanionStatsPacket;
import com.azscompanions.network.packet.TeamFightHudPacket;
import com.azscompanions.teamfight.ClientTeamFightHud;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-only S2C handlers for Forge SimpleChannel.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientNetworkHandlers {
    private ClientNetworkHandlers() {
    }

    public static void handleDialogue(CompanionDialoguePacket packet) {
        ClientVoiceController.handleDialogue(packet.entityId(), packet.category(), packet.line(), packet.voiceProfile());
    }

    public static void handleOpenCreator(OpenCompanionCreatorPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(packet.entityId());
        if (entity instanceof CompanionEntity companion) {
            mc.setScreen(new CompanionCreatorScreen(companion, null));
        }
    }

    public static void handleOpenMenu(OpenCompanionMenuPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(packet.entityId());
        if (entity instanceof CompanionEntity companion) {
            mc.setScreen(new CompanionMenuScreen(companion));
        }
    }

    public static void handleOpenPersona(OpenCompanionPersonaPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(packet.entityId());
        if (entity instanceof CompanionEntity companion) {
            mc.setScreen(new CompanionPersonaScreen(
                    companion,
                    packet.whoAmI(),
                    packet.whatAmIDoing(),
                    packet.howWillIBe(),
                    packet.speechStyle(),
                    packet.relationshipToOwner(),
                    packet.quirks()));
        }
    }

    public static void handleOpenStats(OpenCompanionStatsPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity entity = mc.level.getEntity(packet.entityId());
        if (entity instanceof CompanionEntity companion) {
            mc.setScreen(new CompanionStatsScreen(
                    companion,
                    null,
                    packet.whoAmI(),
                    packet.whatAmIDoing(),
                    packet.howWillIBe(),
                    packet.childCount(),
                    packet.ownedCount(),
                    packet.charmStatus(),
                    packet.aiStatus()));
        }
    }

    public static void handleOpenAdmin(OpenAzAdminPacket packet) {
        Minecraft.getInstance().setScreen(new AzAdminScreen(
                com.azscompanions.admin.AdminAiConfigSnapshot.fromWireJson(packet.aiJson()),
                packet.aiStatus(),
                packet.chunkLoading(),
                packet.teamfight(),
                packet.companionSummary()));
    }

    public static void handleTeamFightHud(TeamFightHudPacket packet) {
        ClientTeamFightHud.apply(packet.payload());
    }

    public static void handleAiThinking(CompanionAiThinkingPacket packet) {
        ClientCompanionAiHud.apply(packet.active(), packet.companionName(), packet.timeoutSeconds(), packet.progress());
    }

    public static void handleDepositSelection(DepositSelectionSyncPacket packet) {
        ClientDepositSelection.apply(packet.payload());
    }

    public static void handleAiJoinOffer(CompanionAiJoinOfferPacket packet) {
        com.azscompanions.client.NeoAiJoinOfferClient.onOffer(packet.toOffer());
    }
}
