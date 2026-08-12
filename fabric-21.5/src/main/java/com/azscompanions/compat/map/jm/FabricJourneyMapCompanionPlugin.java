package com.azscompanions.compat.map.jm;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.compat.map.CompanionMapEntity;
import com.azscompanions.compat.map.MapCompat;
import com.azscompanions.compat.map.MapCompatSettings;
import com.azscompanions.entity.FabricCompanionEntity;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.entity.WrappedEntity;
import journeymap.api.v2.client.event.EntityRadarUpdateEvent;
import journeymap.api.v2.client.event.EntityRegistrationEvent;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Soft JourneyMap plugin (Fabric). Annotation-discovered when JourneyMap is installed.
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public final class FabricJourneyMapCompanionPlugin implements IClientPlugin {
    private static final ResourceLocation COMPANION_ICON =
            ResourceLocation.fromNamespaceAndPath(AzsCompanionsFabric.MOD_ID, "textures/entity_icon/companion.png");

    @Override
    public String getModId() {
        return AzsCompanionsFabric.MOD_ID;
    }

    @Override
    public void initialize(IClientAPI api) {
        ClientEventRegistry.ENTITY_REGISTRATION_EVENT.subscribe(AzsCompanionsFabric.MOD_ID, this::onRegisterEntities);
        ClientEventRegistry.ENTITY_RADAR_UPDATE_EVENT.subscribe(AzsCompanionsFabric.MOD_ID, this::onRadarUpdate);
        AzsCompanionsFabric.LOGGER.info("JourneyMap companion plugin initialized (Fabric)");
    }

    private void onRegisterEntities(EntityRegistrationEvent event) {
        event.addVillagerEntity(FabricCompanionEntity.class);
    }

    private void onRadarUpdate(EntityRadarUpdateEvent event) {
        WrappedEntity wrapped = event.getWrappedEntity();
        Entity entity = wrapped.getEntityRef() == null ? null : wrapped.getEntityRef().get();
        if (!CompanionMapEntity.isCompanion(entity) && !(entity instanceof FabricCompanionEntity)) {
            return;
        }
        boolean child = entity instanceof FabricCompanionEntity companion
                ? companion.isChildCompanion()
                : CompanionMapEntity.isChildCompanion(entity);
        if (!MapCompat.shouldShowOnMap(child)) {
            event.cancel();
            wrapped.setDisable(true);
            return;
        }
        MapCompatSettings settings = MapCompat.settings();
        wrapped.setColor(settings.iconColorArgb());
        wrapped.setLabelColor(settings.iconColorArgb());
        wrapped.setEntityIconLocation(COMPANION_ICON);
        if (settings.showNameOnMap()) {
            String name = entity instanceof FabricCompanionEntity companion
                    ? companion.getChatDisplayName()
                    : CompanionMapEntity.displayName(entity);
            wrapped.setCustomName(Component.literal(name));
        }
        if (settings.showOwnerOnMap()) {
            String owner = CompanionMapEntity.ownerNameHint(entity);
            List<Component> tips = new ArrayList<>();
            if (wrapped.getEntityToolTips() != null) {
                tips.addAll(wrapped.getEntityToolTips());
            }
            if (owner != null && !owner.isBlank()) {
                tips.add(Component.literal("Owner: " + owner));
            }
            if (child) {
                tips.add(Component.literal("Bit"));
            }
            if (!tips.isEmpty()) {
                wrapped.setEntityToolTips(tips);
            }
        }
    }
}
