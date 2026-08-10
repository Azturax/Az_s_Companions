package com.azscompanions.client.network;

import com.azscompanions.ai.ClientCompanionAiHud;
import com.azscompanions.client.screen.AzAdminScreen;
import com.azscompanions.client.screen.CompanionCreatorScreen;
import com.azscompanions.client.screen.CompanionMenuScreen;
import com.azscompanions.client.screen.CompanionPersonaScreen;
import com.azscompanions.client.screen.CompanionStatsScreen;
import com.azscompanions.client.voice.ClientVoiceController;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.deposit.ClientDepositSelection;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Client-only S2C payload handlers. Must not be classloaded on the dedicated server —
 * {@link com.azscompanions.network.ModNetworking} gates registration with {@code FMLEnvironment.dist}.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientNetworkHandlers {
    private ClientNetworkHandlers() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(CompanionDialoguePacket.TYPE, CompanionDialoguePacket.STREAM_CODEC, ClientNetworkHandlers::handleDialogue);
        registrar.playToClient(OpenCompanionCreatorPacket.TYPE, OpenCompanionCreatorPacket.STREAM_CODEC, ClientNetworkHandlers::handleOpenCreator);
        registrar.playToClient(OpenCompanionMenuPacket.TYPE, OpenCompanionMenuPacket.STREAM_CODEC, ClientNetworkHandlers::handleOpenMenu);
        registrar.playToClient(OpenCompanionPersonaPacket.TYPE, OpenCompanionPersonaPacket.STREAM_CODEC, ClientNetworkHandlers::handleOpenPersona);
        registrar.playToClient(OpenCompanionStatsPacket.TYPE, OpenCompanionStatsPacket.STREAM_CODEC, ClientNetworkHandlers::handleOpenStats);
        registrar.playToClient(OpenAzAdminPacket.TYPE, OpenAzAdminPacket.STREAM_CODEC, ClientNetworkHandlers::handleOpenAdmin);
        registrar.playToClient(TeamFightHudPacket.TYPE, TeamFightHudPacket.STREAM_CODEC, ClientNetworkHandlers::handleTeamFightHud);
        registrar.playToClient(CompanionAiThinkingPacket.TYPE, CompanionAiThinkingPacket.STREAM_CODEC, ClientNetworkHandlers::handleAiThinking);
        registrar.playToClient(DepositSelectionSyncPacket.TYPE, DepositSelectionSyncPacket.STREAM_CODEC, ClientNetworkHandlers::handleDepositSelection);
    }

    private static void handleDialogue(CompanionDialoguePacket packet, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientVoiceController.handleDialogue(packet.entityId(), packet.category(), packet.line(), packet.voiceProfile()));
    }

    private static void handleOpenCreator(OpenCompanionCreatorPacket packet, IPayloadContext context) {
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

    private static void handleOpenMenu(OpenCompanionMenuPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }
            Entity entity = mc.level.getEntity(packet.entityId());
            if (entity instanceof CompanionEntity companion) {
                mc.setScreen(new CompanionMenuScreen(companion));
            }
        });
    }

    private static void handleOpenPersona(OpenCompanionPersonaPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
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
        });
    }

    private static void handleOpenStats(OpenCompanionStatsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
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
        });
    }

    private static void handleOpenAdmin(OpenAzAdminPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new AzAdminScreen(
                    com.azscompanions.admin.AdminAiConfigSnapshot.fromWireJson(packet.aiJson()),
                    packet.aiStatus(),
                    packet.chunkLoading(),
                    packet.teamfight(),
                    packet.companionSummary()));
        });
    }

    private static void handleTeamFightHud(TeamFightHudPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientTeamFightHud.apply(packet.payload()));
    }

    private static void handleAiThinking(CompanionAiThinkingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientCompanionAiHud.apply(
                packet.active(), packet.companionName(), packet.timeoutSeconds(), packet.progress()));
    }

    private static void handleDepositSelection(DepositSelectionSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientDepositSelection.apply(packet.payload()));
    }
}
