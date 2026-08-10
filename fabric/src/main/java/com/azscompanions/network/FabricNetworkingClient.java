package com.azscompanions.network;

import com.azscompanions.client.screen.FabricCompanionMenuScreen;
import com.azscompanions.entity.FabricCompanionEntity;
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
}
