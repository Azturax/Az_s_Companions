package com.azscompanions.client;

import com.azscompanions.AzsCompanions;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final String CATEGORY = "key.categories." + AzsCompanions.MOD_ID;

    /** Default {@code V} — opens the companion command radial. */
    public static final KeyMapping OPEN_RADIAL = new KeyMapping(
            "key.azscompanions.open_radial",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    private ModKeyMappings() {
    }
}
