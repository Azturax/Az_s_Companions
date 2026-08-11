package com.azscompanions.network;

import com.azscompanions.ai.ClientCompanionAiHud;
import com.azscompanions.client.screen.FabricCompanionMenuScreen;
import com.azscompanions.client.screen.FabricCompanionPersonaScreen;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.teamfight.ClientTeamFightHud;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public final class FabricNetworkingClient {
    private FabricNetworkingClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.OpenMenuPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.level == null) {
                        return;
                    }
                    Entity entity = mc.level.getEntity(payload.entityId());
                    if (entity instanceof FabricCompanionEntity companion) {
                        mc.setScreen(new FabricCompanionMenuScreen(companion));
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.OpenPersonaPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
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
                }));
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.OpenStatsPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
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
                }));
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.TeamFightHudPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientTeamFightHud.apply(payload.payload())));
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.AiThinkingPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientCompanionAiHud.apply(
                        payload.active(), payload.companionName(), payload.timeoutSeconds(), payload.progress())));
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.DepositSelectionPayload.TYPE, (payload, context) ->
                context.client().execute(() -> com.azscompanions.deposit.ClientDepositSelection.apply(payload.payload())));
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.OpenAdminPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.setScreen(new com.azscompanions.client.screen.FabricAzAdminScreen(
                            com.azscompanions.admin.AdminAiConfigSnapshot.fromWireJson(payload.aiJson()),
                            payload.aiStatus(),
                            payload.chunkLoading(),
                            payload.teamfight(),
                            payload.companionSummary()));
                }));
        ClientPlayNetworking.registerGlobalReceiver(FabricNetworking.AiJoinOfferPayload.TYPE, (payload, context) ->
                context.client().execute(() ->
                        com.azscompanions.client.FabricAiJoinOfferClient.onOffer(payload.toOffer())));
    }

    public static void sendRecruit(String definitionId) {
        ClientPlayNetworking.send(new FabricNetworking.RecruitPayload(definitionId));
    }

    public static void sendSettings(FabricNetworking.SettingsPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void sendMenuAction(int entityId, String action) {
        ClientPlayNetworking.send(new FabricNetworking.MenuActionPayload(entityId, action));
    }

    public static void sendBehavior(int entityId, float followRadius, float personalSpace, float wanderRadius) {
        ClientPlayNetworking.send(new FabricNetworking.BehaviorPayload(entityId, followRadius, personalSpace, wanderRadius));
    }

    public static void sendPersona(FabricNetworking.PersonaPayload payload) {
        ClientPlayNetworking.send(payload);
    }

    public static void sendAdminAiSave(com.azscompanions.admin.AdminAiConfigSnapshot snap) {
        String json = snap == null ? "{}" : snap.toWireJson();
        ClientPlayNetworking.send(new FabricNetworking.AdminAiSavePayload(json));
    }

    public static void sendAdminAction(String action) {
        ClientPlayNetworking.send(new FabricNetworking.AdminActionPayload(action == null ? "" : action));
    }

    public static void sendAiJoinConsent(boolean accepted, String suggestProfile, boolean applyProfile) {
        ClientPlayNetworking.send(new FabricNetworking.AiJoinConsentPayload(
                accepted,
                suggestProfile == null ? "" : suggestProfile,
                applyProfile));
    }
}
