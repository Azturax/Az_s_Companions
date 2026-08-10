package com.azscompanions.client.screen;

import com.azscompanions.entity.CompanionEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Minimal companion stats screen stub for NeoForge 26.2. */
public final class CompanionStatsScreen extends Screen {
    private final CompanionEntity companion;
    private final String who;
    private final String what;
    private final String how;

    public CompanionStatsScreen(
            CompanionEntity companion,
            Screen parent,
            String who,
            String what,
            String how
    ) {
        super(Component.literal("Companion Stats"));
        this.companion = companion;
        this.who = who == null ? "" : who;
        this.what = what == null ? "" : what;
        this.how = how == null ? "" : how;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(width / 2 - 40, height - 40, 80, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.text(font, companion.getDisplayName(), width / 2 - 60, 40, 0xFFFFFF, false);
        graphics.text(font, "Who: " + who, width / 2 - 120, 70, 0xA0A0A0, false);
        graphics.text(font, "What: " + what, width / 2 - 120, 90, 0xA0A0A0, false);
        graphics.text(font, "How: " + how, width / 2 - 120, 110, 0xA0A0A0, false);
    }
}
