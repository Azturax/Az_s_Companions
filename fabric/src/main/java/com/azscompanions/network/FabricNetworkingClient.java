package com.azscompanions.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public final class FabricNetworkingClient {
    private FabricNetworkingClient() {
    }

    public static void sendRecruit(String definitionId) {
        ClientPlayNetworking.send(new FabricNetworking.RecruitPayload(definitionId));
    }

    public static void sendRadial(int entityId, String command) {
        ClientPlayNetworking.send(new FabricNetworking.RadialPayload(entityId, command));
    }
}
