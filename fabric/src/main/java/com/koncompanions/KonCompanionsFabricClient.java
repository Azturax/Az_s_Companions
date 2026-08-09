package com.koncompanions;

import com.koncompanions.client.model.FeminineCompanionModel;
import com.koncompanions.client.renderer.FabricCompanionRenderer;
import com.koncompanions.client.screen.FabricCompanionInventoryScreen;
import com.koncompanions.client.screen.FabricCompanionSelectionScreen;
import com.koncompanions.client.screen.FabricRadialCommandScreen;
import com.koncompanions.registry.FabricModEntities;
import com.koncompanions.registry.FabricModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public final class KonCompanionsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(FeminineCompanionModel.LAYER_WIDE,
                () -> FeminineCompanionModel.createBodyLayer(false));
        EntityModelLayerRegistry.registerModelLayer(FeminineCompanionModel.LAYER_SLIM,
                () -> FeminineCompanionModel.createBodyLayer(true));
        EntityRendererRegistry.register(FabricModEntities.COMPANION, FabricCompanionRenderer::new);
        MenuScreens.register(FabricModScreenHandlers.SELECTION, FabricCompanionSelectionScreen::new);
        MenuScreens.register(FabricModScreenHandlers.RADIAL, FabricRadialCommandScreen::new);
        MenuScreens.register(FabricModScreenHandlers.INVENTORY, FabricCompanionInventoryScreen::new);
        KonCompanionsFabric.LOGGER.info("Kon Companions (Fabric) client ready");
    }
}
