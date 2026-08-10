package com.azscompanions.client.screen;

import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.network.FabricNetworking;
import com.azscompanions.network.FabricNetworkingClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** First-create / revisit Who–What–How persona setup. Owner only. */
public final class FabricCompanionPersonaScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;

    private final FabricCompanionEntity companion;
    private final String initialWho;
    private final String initialWhat;
    private final String initialHow;
    private EditBox whoBox;
    private EditBox whatBox;
    private EditBox howBox;
    private boolean submitted;

    public FabricCompanionPersonaScreen(FabricCompanionEntity companion, String who, String what, String how) {
        super(Component.literal("Persona setup"));
        this.companion = companion;
        this.initialWho = who == null ? "" : who;
        this.initialWhat = what == null ? "" : what;
        this.initialHow = how == null ? "" : how;
    }

    @Override
    protected void init() {
        int panelW = 280;
        int panelH = 180;
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;
        int bx = px + 16;
        int bw = panelW - 32;

        whoBox = new EditBox(font, bx, py + 28, bw, 18, Component.literal("Who am I"));
        whoBox.setMaxLength(CompanionPersona.MAX_LEN);
        whoBox.setValue(initialWho);
        whoBox.setHint(Component.literal("Who am I? identity / backstory"));
        addRenderableWidget(whoBox);

        whatBox = new EditBox(font, bx, py + 62, bw, 18, Component.literal("What am I doing"));
        whatBox.setMaxLength(CompanionPersona.MAX_LEN);
        whatBox.setValue(initialWhat);
        whatBox.setHint(Component.literal("What am I doing? purpose / goal"));
        addRenderableWidget(whatBox);

        howBox = new EditBox(font, bx, py + 96, bw, 18, Component.literal("How will I be"));
        howBox.setMaxLength(CompanionPersona.MAX_LEN);
        howBox.setValue(initialHow);
        howBox.setHint(Component.literal("How will I be? personality / tone"));
        addRenderableWidget(howBox);

        addRenderableWidget(Button.builder(Component.literal("Save"), b -> submit(false))
                .bounds(bx, py + panelH - 48, (bw - 8) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Skip"), b -> submit(true))
                .bounds(bx + (bw - 8) / 2 + 8, py + panelH - 48, (bw - 8) / 2, 20).build());
    }

    private void submit(boolean skip) {
        if (submitted || companion == null) {
            return;
        }
        submitted = true;
        FabricNetworkingClient.sendPersona(new FabricNetworking.PersonaPayload(
                companion.getId(),
                whoBox != null ? whoBox.getValue() : "",
                whatBox != null ? whatBox.getValue() : "",
                howBox != null ? howBox.getValue() : "",
                skip));
        onClose();
    }

    @Override
    public void onClose() {
        if (!submitted && companion != null) {
            submitted = true;
            FabricNetworkingClient.sendPersona(new FabricNetworking.PersonaPayload(
                    companion.getId(), "", "", "", true));
        }
        super.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int panelW = 280;
        int panelH = 180;
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;
        graphics.fill(px - 1, py - 1, px + panelW + 1, py + panelH + 1, PANEL_EDGE);
        graphics.fill(px, py, px + panelW, py + panelH, PANEL_BG);
        graphics.drawString(font, "Persona — " + companion.getChatDisplayName(), px + 16, py + 10, 0xFFFFFF, false);
        graphics.drawString(font, "Who", px + 16, py + 18, 0xA0A0A0, false);
        graphics.drawString(font, "What", px + 16, py + 52, 0xA0A0A0, false);
        graphics.drawString(font, "How", px + 16, py + 86, 0xA0A0A0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
