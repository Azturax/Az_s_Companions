package com.azscompanions.client;

import com.azscompanions.client.screen.FabricCompanionCommandScreen;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.network.FabricNetworkingClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** Fabric keybinds — registered so they appear under Options → Controls. */
public final class FabricModKeyMappings {
    public static final String CATEGORY = "key.categories.azscompanions";

    public static final KeyMapping OPEN_COMMAND_MENU = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.azscompanions.open_command_menu",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_K,
                    CATEGORY));

    public static final KeyMapping TOGGLE_WIGGLY_DOG = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.azscompanions.toggle_wiggly_dog",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_H,
                    CATEGORY));

    private FabricModKeyMappings() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_COMMAND_MENU.consumeClick()) {
                if (client.screen != null || client.player == null || client.level == null) {
                    continue;
                }
                FabricCompanionEntity companion = findTarget(client);
                if (companion == null) {
                    client.player.displayClientMessage(
                            Component.translatable("message.azscompanions.command_no_companion"), true);
                    continue;
                }
                client.setScreen(new FabricCompanionCommandScreen(companion, null));
            }
            while (TOGGLE_WIGGLY_DOG.consumeClick()) {
                if (client.screen != null || client.player == null) {
                    continue;
                }
                if (!com.azscompanions.perk.WigglyDogPerkSupport.isEligible(client.player.getUUID())) {
                    client.player.displayClientMessage(
                            Component.translatable("message.azscompanions.wiggly_dog_denied"), true);
                    continue;
                }
                FabricNetworkingClient.sendToggleWigglyDog();
            }
        });
    }

    @Nullable
    private static FabricCompanionEntity findTarget(Minecraft mc) {
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return null;
        }
        if (mc.crosshairPickEntity instanceof FabricCompanionEntity looked
                && canCommand(looked, player)
                && looked.distanceTo(player) <= 64.0d) {
            return looked;
        }
        FabricCompanionEntity best = null;
        double bestDist = 32.0d * 32.0d;
        for (FabricCompanionEntity companion : mc.level.getEntitiesOfClass(
                FabricCompanionEntity.class,
                player.getBoundingBox().inflate(32.0d),
                e -> canCommand(e, player))) {
            double dist = companion.distanceToSqr(player);
            if (dist < bestDist) {
                bestDist = dist;
                best = companion;
            }
        }
        return best;
    }

    private static boolean canCommand(FabricCompanionEntity companion, Player player) {
        return companion.isOwnedBy(player) || companion.isTrusted(player);
    }
}
