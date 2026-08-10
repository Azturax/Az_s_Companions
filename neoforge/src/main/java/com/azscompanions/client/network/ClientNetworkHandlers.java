package com.azscompanions.client.network;

import com.azscompanions.client.screen.CompanionCreatorScreen;
import com.azscompanions.client.screen.CompanionMenuScreen;
import com.azscompanions.client.voice.ClientVoiceController;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.network.packet.CompanionDialoguePacket;
import com.azscompanions.network.packet.OpenCompanionCreatorPacket;
import com.azscompanions.network.packet.OpenCompanionMenuPacket;
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
        registrar.playToClient(TeamFightHudPacket.TYPE, TeamFightHudPacket.STREAM_CODEC, ClientNetworkHandlers::handleTeamFightHud);
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

    private static void handleTeamFightHud(TeamFightHudPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientTeamFightHud.apply(packet.payload()));
    }
}
