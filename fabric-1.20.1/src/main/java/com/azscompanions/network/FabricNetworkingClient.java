package com.azscompanions.network;

import com.azscompanions.ai.ClientCompanionAiHud;
import com.azscompanions.client.screen.FabricCompanionMenuScreen;
import com.azscompanions.client.screen.FabricCompanionPersonaScreen;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.teamfight.ClientTeamFightHud;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class FabricNetworkingClient {
    private FabricNetworkingClient() {
    }

    private static void send(ResourceLocation id, Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        writer.accept(buf);
        ClientPlayNetworking.send(id, buf);
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.OpenMenuPayload.ID, (client, handler, buf, responseSender) -> {
            FabricNetworking.OpenMenuPayload payload = FabricNetworking.OpenMenuPayload.read(buf);
            client.execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                    return;
                }
                Entity entity = mc.level.getEntity(payload.entityId());
                if (entity instanceof FabricCompanionEntity companion) {
                    mc.setScreen(new FabricCompanionMenuScreen(companion));
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.OpenPersonaPayload.ID, (client, handler, buf, responseSender) -> {
            FabricNetworking.OpenPersonaPayload payload = FabricNetworking.OpenPersonaPayload.read(buf);
            client.execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                    return;
                }
                Entity entity = mc.level.getEntity(payload.entityId());
                if (entity instanceof FabricCompanionEntity companion) {
                    mc.setScreen(new FabricCompanionPersonaScreen(
                            companion,
                            payload.whoAmI(),
                            payload.whatAmIDoing(),
                            payload.howWillIBe(),
                            payload.speechStyle(),
                            payload.relationshipToOwner(),
                            payload.quirks()));
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.OpenStatsPayload.ID, (client, handler, buf, responseSender) -> {
            FabricNetworking.OpenStatsPayload payload = FabricNetworking.OpenStatsPayload.read(buf);
            client.execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) {
                    return;
                }
                Entity entity = mc.level.getEntity(payload.entityId());
                if (entity instanceof FabricCompanionEntity companion) {
                    mc.setScreen(new com.azscompanions.client.screen.FabricCompanionStatsScreen(
                            companion,
                            null,
                            payload.whoAmI(),
                            payload.whatAmIDoing(),
                            payload.howWillIBe(),
                            payload.childCount(),
                            payload.ownedCount(),
                            payload.charmStatus(),
                            payload.aiStatus()));
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.TeamFightHudPayload.ID, (client, handler, buf, responseSender) -> {
            FabricNetworking.TeamFightHudPayload payload = FabricNetworking.TeamFightHudPayload.read(buf);
            client.execute(() -> ClientTeamFightHud.apply(payload.payload()));
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.AiThinkingPayload.ID, (client, handler, buf, responseSender) -> {
            FabricNetworking.AiThinkingPayload payload = FabricNetworking.AiThinkingPayload.read(buf);
            client.execute(() -> ClientCompanionAiHud.apply(
                    payload.active(), payload.companionName(), payload.timeoutSeconds(), payload.progress()));
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.DepositSelectionPayload.ID, (client, handler, buf, responseSender) -> {
            FabricNetworking.DepositSelectionPayload payload = FabricNetworking.DepositSelectionPayload.read(buf);
            client.execute(() -> com.azscompanions.deposit.ClientDepositSelection.apply(payload.payload()));
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.OpenAdminPayload.ID, (client, handler, buf, responseSender) -> {
            FabricNetworking.OpenAdminPayload payload = FabricNetworking.OpenAdminPayload.read(buf);
            client.execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new com.azscompanions.client.screen.FabricAzAdminScreen(
                        com.azscompanions.admin.AdminAiConfigSnapshot.fromWireJson(payload.aiJson()),
                        payload.aiStatus(),
                        payload.chunkLoading(),
                        payload.teamfight(),
                        payload.companionSummary()));
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.AiJoinOfferPayload.ID, (client, handler, buf, responseSender) -> {
            FabricNetworking.AiJoinOfferPayload payload = FabricNetworking.AiJoinOfferPayload.read(buf);
            client.execute(() -> com.azscompanions.client.FabricAiJoinOfferClient.onOffer(payload.toOffer()));
        });
    }

    public static void sendRecruit(String definitionId) {
        send(FabricNetworking.RecruitPayload.ID, buf ->
                FabricNetworking.RecruitPayload.write(buf, new FabricNetworking.RecruitPayload(definitionId)));
    }

    public static void sendSettings(FabricNetworking.SettingsPayload payload) {
        send(FabricNetworking.SettingsPayload.ID, buf -> FabricNetworking.SettingsPayload.write(buf, payload));
    }

    public static void sendContextSkins(FabricNetworking.ContextSkinsPayload payload) {
        send(FabricNetworking.ContextSkinsPayload.ID, buf -> FabricNetworking.ContextSkinsPayload.write(buf, payload));
    }

    public static void sendMenuAction(int entityId, String action) {
        send(FabricNetworking.MenuActionPayload.ID, buf ->
                FabricNetworking.MenuActionPayload.write(buf, new FabricNetworking.MenuActionPayload(entityId, action)));
    }

    public static void sendBehavior(int entityId, float followRadius, float personalSpace, float wanderRadius) {
        send(FabricNetworking.BehaviorPayload.ID, buf ->
                FabricNetworking.BehaviorPayload.write(buf,
                        new FabricNetworking.BehaviorPayload(entityId, followRadius, personalSpace, wanderRadius)));
    }

    public static void sendPersona(FabricNetworking.PersonaPayload payload) {
        send(FabricNetworking.PersonaPayload.ID, buf -> FabricNetworking.PersonaPayload.write(buf, payload));
    }

    public static void sendAdminAiSave(com.azscompanions.admin.AdminAiConfigSnapshot snap) {
        String json = snap == null ? "{}" : snap.toWireJson();
        send(FabricNetworking.AdminAiSavePayload.ID, buf ->
                FabricNetworking.AdminAiSavePayload.write(buf, new FabricNetworking.AdminAiSavePayload(json)));
    }

    public static void sendAdminAction(String action) {
        send(FabricNetworking.AdminActionPayload.ID, buf ->
                FabricNetworking.AdminActionPayload.write(buf,
                        new FabricNetworking.AdminActionPayload(action == null ? "" : action)));
    }

    public static void sendAiJoinConsent(boolean accepted, String suggestProfile, boolean applyProfile) {
        send(FabricNetworking.AiJoinConsentPayload.ID, buf ->
                FabricNetworking.AiJoinConsentPayload.write(buf, new FabricNetworking.AiJoinConsentPayload(
                        accepted,
                        suggestProfile == null ? "" : suggestProfile,
                        applyProfile)));
    }

    public static void sendToggleWigglyDog() {
        send(FabricNetworking.ToggleWigglyDogPayload.ID, buf ->
                FabricNetworking.ToggleWigglyDogPayload.write(buf, new FabricNetworking.ToggleWigglyDogPayload()));
    }
}
