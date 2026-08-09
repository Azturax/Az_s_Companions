package com.azscompanions.client;

import com.azscompanions.AzsCompanionsFabric;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class FabricModKeyMappings {
    public static final String CATEGORY = "key.categories." + AzsCompanionsFabric.MOD_ID;

    public static KeyMapping OPEN_RADIAL;

    private FabricModKeyMappings() {
    }

    public static void register() {
        OPEN_RADIAL = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.azscompanions.open_radial",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        ));
    }
}
