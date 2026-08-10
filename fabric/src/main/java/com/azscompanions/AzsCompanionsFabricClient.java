package com.azscompanions;

import com.azscompanions.client.hud.TeamFightHudOverlay;
import com.azscompanions.client.model.FeminineCompanionModel;
import com.azscompanions.client.model.KonEarsModel;
import com.azscompanions.client.renderer.FabricCompanionRenderer;
import com.azscompanions.client.renderer.KonEarsLayer;
import com.azscompanions.client.screen.FabricCompanionInventoryScreen;
import com.azscompanions.client.screen.FabricCompanionSelectionScreen;
import com.azscompanions.network.FabricNetworkingClient;
import com.azscompanions.registry.FabricModEntities;
import com.azscompanions.registry.FabricModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public final class AzsCompanionsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(FeminineCompanionModel.LAYER_WIDE,
                () -> FeminineCompanionModel.createBodyLayer(false));
        EntityModelLayerRegistry.registerModelLayer(FeminineCompanionModel.LAYER_SLIM,
                () -> FeminineCompanionModel.createBodyLayer(true));
        EntityModelLayerRegistry.registerModelLayer(KonEarsModel.LAYER, KonEarsModel::createBodyLayer);
        EntityRendererRegistry.register(FabricModEntities.COMPANION, FabricCompanionRenderer::new);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (entityType, entityRenderer, registrationHelper, context) -> {
                    if (entityRenderer instanceof PlayerRenderer playerRenderer) {
                        registrationHelper.register(new KonEarsLayer(playerRenderer, context.getModelSet()));
                    }
                });
        MenuScreens.register(FabricModScreenHandlers.SELECTION, FabricCompanionSelectionScreen::new);
        MenuScreens.register(FabricModScreenHandlers.INVENTORY, FabricCompanionInventoryScreen::new);
        FabricNetworkingClient.register();
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> TeamFightHudOverlay.render(graphics, 0f));
        com.azscompanions.compat.map.FabricMapCompat.bootstrapClient();
        com.azscompanions.compat.fancyanim.FabricFancyAnimCompat.bootstrapClient();

        AzsCompanionsFabric.LOGGER.info("Az's Companions (Fabric) client ready");
    }
}
