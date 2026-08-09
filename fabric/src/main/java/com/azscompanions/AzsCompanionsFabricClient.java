package com.azscompanions;

import com.azscompanions.client.model.FeminineCompanionModel;
import com.azscompanions.client.renderer.FabricCompanionRenderer;
import com.azscompanions.client.screen.FabricCompanionInventoryScreen;
import com.azscompanions.client.screen.FabricCompanionSelectionScreen;
import com.azscompanions.client.screen.FabricRadialCommandScreen;
import com.azscompanions.registry.FabricModEntities;
import com.azscompanions.registry.FabricModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public final class AzsCompanionsFabricClient implements ClientModInitializer {
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
        AzsCompanionsFabric.LOGGER.info("Az's Companions (Fabric) client ready");
    }
}
