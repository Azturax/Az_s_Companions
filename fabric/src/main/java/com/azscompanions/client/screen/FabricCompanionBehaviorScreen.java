package com.azscompanions.client.screen;

import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.network.FabricNetworkingClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Follow / personal-space / wander sliders. Values sync to the companion entity. */
public final class FabricCompanionBehaviorScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;

    private final FabricCompanionEntity companion;
    private final Screen parent;
    private float followRadius;
    private float personalSpace;
    private float wanderRadius;
    private int panelX;
    private int panelY;
    private final int panelW = 240;
    private final int panelH = 200;

    public FabricCompanionBehaviorScreen(FabricCompanionEntity companion, Screen parent) {
        super(Component.translatable("screen.azscompanions.behavior"));
        this.companion = companion;
        this.parent = parent;
        this.followRadius = companion.getFollowRadius();
        this.personalSpace = companion.getPersonalSpace();
        this.wanderRadius = companion.getWanderRadius();
    }

    @Override
    protected void init() {
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int bx = panelX + 20;
        int by = panelY + 36;
        int bw = panelW - 40;
        by = addSlider(bx, by, bw, "screen.azscompanions.behavior.follow_radius",
                () -> followRadius, v -> followRadius = v,
                CompanionFollowDistances.FOLLOW_RADIUS_MIN, CompanionFollowDistances.FOLLOW_RADIUS_MAX, 1.0f);
        by = addSlider(bx, by + 6, bw, "screen.azscompanions.behavior.personal_space",
                () -> personalSpace, v -> personalSpace = v,
                CompanionFollowDistances.PERSONAL_SPACE_MIN, CompanionFollowDistances.PERSONAL_SPACE_MAX, 0.5f);
        by = addSlider(bx, by + 6, bw, "screen.azscompanions.behavior.wander_radius",
                () -> wanderRadius, v -> wanderRadius = v,
                CompanionFollowDistances.WANDER_RADIUS_MIN, CompanionFollowDistances.WANDER_RADIUS_MAX, 1.0f);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> {
            push();
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }).bounds(bx, panelY + panelH - 48, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }).bounds(bx, panelY + panelH - 24, bw, 18).build());
    }

    private int addSlider(int x, int y, int w, String langKey, Supplier<Float> getter, Consumer<Float> setter,
                          float min, float max, float step) {
        double initial = Math.max(0.0d, Math.min(1.0d, (getter.get() - min) / (max - min)));
        AbstractSliderButton slider = new AbstractSliderButton(x, y, w, 20, Component.empty(), initial) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.translatable(langKey, String.format(Locale.ROOT, "%.1f", getter.get())));
            }

            @Override
            protected void applyValue() {
                float v = min + (float) (this.value * (max - min));
                v = Math.round(v / step) * step;
                setter.accept(Math.max(min, Math.min(max, v)));
                updateMessage();
                push();
            }
        };
        addRenderableWidget(slider);
        return y + 22;
    }

    private void push() {
        FabricNetworkingClient.sendBehavior(companion.getId(), followRadius, personalSpace, wanderRadius);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.drawCenteredString(font, title, panelX + panelW / 2, panelY + 12, 0xFFFFFF);
        graphics.drawCenteredString(font, companion.getDisplayName().getString(),
                panelX + panelW / 2, panelY + 24, 0xA0A0A0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
