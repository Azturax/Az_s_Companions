package com.azscompanions.client.screen;

import com.azscompanions.ai.CompanionPersona;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.network.packet.CompanionPersonaPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** First-create / revisit persona setup (all fields). Owner only. Scrollable when needed. */
public final class CompanionPersonaScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;
    private static final int SCROLLBAR_W = 4;
    private static final int FIELD_H = 18;
    private static final int LABEL_H = 10;
    private static final int ROW_GAP = 36;

    private final CompanionEntity companion;
    private final String initialWho;
    private final String initialWhat;
    private final String initialHow;
    private final String initialSpeech;
    private final String initialRelationship;
    private final String initialQuirks;

    private EditBox whoBox;
    private EditBox whatBox;
    private EditBox howBox;
    private EditBox speechBox;
    private EditBox relationshipBox;
    private EditBox quirksBox;
    private boolean submitted;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int viewTop;
    private int viewBottom;
    private int scroll;
    private int maxScroll;
    private final List<ScrollEntry> scrollEntries = new ArrayList<>();
    private final List<ScrollLabel> scrollLabels = new ArrayList<>();

    public CompanionPersonaScreen(
            CompanionEntity companion,
            String who,
            String what,
            String how,
            String speech,
            String relationship,
            String quirks
    ) {
        super(Component.translatable("screen.azscompanions.persona.title"));
        this.companion = companion;
        this.initialWho = who == null ? "" : who;
        this.initialWhat = what == null ? "" : what;
        this.initialHow = how == null ? "" : how;
        this.initialSpeech = speech == null ? "" : speech;
        this.initialRelationship = relationship == null ? "" : relationship;
        this.initialQuirks = quirks == null ? "" : quirks;
    }

    @Override
    protected void init() {
        scrollEntries.clear();
        scrollLabels.clear();

        panelW = 300;
        panelH = Math.min(260, Math.max(180, height - 40));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int bx = panelX + 16;
        int bw = panelW - 32 - SCROLLBAR_W - 4;
        viewTop = panelY + 28;
        viewBottom = panelY + panelH - 36;

        int y = 0;
        whoBox = addField(bx, bw, y, "screen.azscompanions.persona.who",
                "screen.azscompanions.persona.who.hint", initialWho);
        y += ROW_GAP;
        whatBox = addField(bx, bw, y, "screen.azscompanions.persona.what",
                "screen.azscompanions.persona.what.hint", initialWhat);
        y += ROW_GAP;
        howBox = addField(bx, bw, y, "screen.azscompanions.persona.how",
                "screen.azscompanions.persona.how.hint", initialHow);
        y += ROW_GAP;
        speechBox = addField(bx, bw, y, "screen.azscompanions.persona.speech",
                "screen.azscompanions.persona.speech.hint", initialSpeech);
        y += ROW_GAP;
        relationshipBox = addField(bx, bw, y, "screen.azscompanions.persona.relationship",
                "screen.azscompanions.persona.relationship.hint", initialRelationship);
        y += ROW_GAP;
        quirksBox = addField(bx, bw, y, "screen.azscompanions.persona.quirks",
                "screen.azscompanions.persona.quirks.hint", initialQuirks);
        y += LABEL_H + FIELD_H + 4;

        int contentH = y;
        int viewH = Math.max(1, viewBottom - viewTop);
        maxScroll = Math.max(0, contentH - viewH);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        applyScroll();

        int half = (bw - 8) / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.persona.save"), b -> submit(false))
                .bounds(bx, panelY + panelH - 28, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.persona.skip"), b -> submit(true))
                .bounds(bx + half + 8, panelY + panelH - 28, half, 20).build());
    }

    private EditBox addField(int x, int w, int contentY, String labelKey, String hintKey, String value) {
        scrollLabels.add(new ScrollLabel(contentY, Component.translatable(labelKey), 0xA0A0A0));
        EditBox box = new EditBox(font, x, 0, w, FIELD_H, Component.translatable(labelKey));
        box.setMaxLength(CompanionPersona.MAX_LEN);
        box.setValue(value);
        box.setHint(Component.translatable(hintKey));
        scrollEntries.add(new ScrollEntry(box, contentY + LABEL_H, FIELD_H));
        addRenderableWidget(box);
        return box;
    }

    private void applyScroll() {
        scroll = Mth.clamp(scroll, 0, maxScroll);
        for (ScrollEntry entry : scrollEntries) {
            int screenY = viewTop + entry.contentY - scroll;
            entry.widget.setY(screenY);
            boolean visible = screenY + entry.height > viewTop && screenY < viewBottom;
            entry.widget.visible = visible;
            entry.widget.active = visible;
        }
    }

    private boolean isOverScrollArea(double mouseX, double mouseY) {
        return mouseX >= panelX && mouseX <= panelX + panelW
                && mouseY >= viewTop && mouseY <= viewBottom;
    }

    private void submit(boolean skip) {
        if (submitted || companion == null) {
            return;
        }
        submitted = true;
        ClientPacketDistributor.sendToServer(new CompanionPersonaPacket(
                companion.getId(),
                valueOf(whoBox),
                valueOf(whatBox),
                valueOf(howBox),
                valueOf(speechBox),
                valueOf(relationshipBox),
                valueOf(quirksBox),
                skip));
        onClose();
    }

    private static String valueOf(EditBox box) {
        return box != null ? box.getValue() : "";
    }

    @Override
    public void onClose() {
        if (!submitted && companion != null) {
            submitted = true;
            ClientPacketDistributor.sendToServer(new CompanionPersonaPacket(
                    companion.getId(), "", "", "", "", "", "", true));
        }
        super.onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll > 0 && isOverScrollArea(mouseX, mouseY)) {
            scroll = Mth.clamp(scroll - (int) (scrollY * 12), 0, maxScroll);
            applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }


    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.text(font,
                Component.translatable("screen.azscompanions.persona.header", companion.getChatDisplayName()),
                panelX + 16, panelY + 10, 0xFFFFFF, false);

        for (ScrollEntry entry : scrollEntries) {
            entry.widget.visible = false;
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.enableScissor(panelX + 8, viewTop, panelX + panelW - 12, viewBottom);
        for (ScrollLabel label : scrollLabels) {
            int screenY = viewTop + label.contentY - scroll;
            if (screenY + 10 < viewTop || screenY > viewBottom) {
                continue;
            }
            graphics.text(font, label.text, panelX + 16, screenY, label.color, false);
        }
        for (ScrollEntry entry : scrollEntries) {
            int screenY = viewTop + entry.contentY - scroll;
            boolean visible = screenY + entry.height > viewTop && screenY < viewBottom;
            entry.widget.visible = visible;
            entry.widget.active = visible;
            if (visible) {
                entry.widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }
        graphics.disableScissor();
        drawScrollbar(graphics);
    }

    private void drawScrollbar(GuiGraphicsExtractor graphics) {
        if (maxScroll <= 0) {
            return;
        }
        int trackX = panelX + panelW - 10;
        int trackH = Math.max(1, viewBottom - viewTop);
        graphics.fill(trackX, viewTop, trackX + SCROLLBAR_W, viewTop + trackH, 0x66000000);
        int thumbH = Math.max(16, (int) (trackH * (trackH / (float) (trackH + maxScroll))));
        int travel = trackH - thumbH;
        int thumbY = viewTop + (travel <= 0 ? 0 : (int) (travel * (scroll / (float) maxScroll)));
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_W, thumbY + thumbH, 0xFFC0C0C0);
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_W, thumbY + 1, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ScrollEntry(AbstractWidget widget, int contentY, int height) {
    }

    private record ScrollLabel(int contentY, Component text, int color) {
    }
}
