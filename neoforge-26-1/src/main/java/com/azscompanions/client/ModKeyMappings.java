package com.azscompanions.client;

import com.azscompanions.AzsCompanions;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Client keybinds — registered so they appear under Options → Controls. */
public final class ModKeyMappings {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(AzsCompanions.MOD_ID, "main"));

    public static final KeyMapping OPEN_COMMAND_MENU = new KeyMapping(
            "key.azscompanions.open_command_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY);

    public static final KeyMapping TOGGLE_WIGGLY_DOG = new KeyMapping(
            "key.azscompanions.toggle_wiggly_dog",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY);

    private ModKeyMappings() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(OPEN_COMMAND_MENU);
        event.register(TOGGLE_WIGGLY_DOG);
    }
}
