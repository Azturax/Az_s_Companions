package com.azscompanions.client.hud;

import com.azscompanions.ai.ClientCompanionAiHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * Top-right HUD: spinning gear + companion Thinking… + soft progress bar.
 */
public final class CompanionAiThinkingHudOverlay {
    private static final int PANEL_W = 148;
    private static final int PANEL_H = 36;
    private static final int MARGIN = 8;

    private CompanionAiThinkingHudOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics, float partialTick) {
        if (!ClientCompanionAiHud.isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (false) {
            return;
        }
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int x = sw - PANEL_W - MARGIN;
        int y = MARGIN;

        graphics.fill(x - 1, y - 1, x + PANEL_W + 1, y + PANEL_H + 1, 0xAA202028);
        graphics.fill(x, y, x + PANEL_W, y + PANEL_H, 0xCC101018);

        int gearCx = x + 14;
        int gearCy = y + 14;
        drawGear(graphics, gearCx, gearCy, ClientCompanionAiHud.gearRadians(partialTick));

        String name = ClientCompanionAiHud.companionName();
        String label = name.length() > 14 ? name.substring(0, 13) + "…" : name;
        graphics.text(font, label, x + 28, y + 4, 0xFFE8E8F0, false);
        graphics.text(font, "Thinking...", x + 28, y + 14, 0xFFB0B0C0, false);

        float progress = ClientCompanionAiHud.progress();
        int barX = x + 8;
        int barY = y + PANEL_H - 10;
        int barW = PANEL_W - 16;
        int barH = 4;
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF303038);
        int fill = Math.max(2, Math.round(barW * Mth.clamp(progress, 0f, 1f)));
        graphics.fill(barX, barY, barX + fill, barY + barH, 0xFF6EC8FF);
        // Soft sweep highlight so indeterminate/long waits still feel alive.
        float sweep = (System.currentTimeMillis() % 1200L) / 1200f;
        int spark = barX + Math.round((barW - 8) * sweep);
        graphics.fill(spark, barY, Math.min(barX + barW, spark + 8), barY + barH, 0xAAFFFFFF);
    }

    /** Procedural gear (no external texture required). */
    private static void drawGear(GuiGraphicsExtractor graphics, int cx, int cy, float radians) {
        int teeth = 8;
        float outer = 7.5f;
        float inner = 4.5f;
        float hub = 2.2f;
        for (int i = 0; i < teeth; i++) {
            float a0 = radians + (i * ((float) Math.PI * 2f / teeth));
            float a1 = a0 + ((float) Math.PI * 2f / teeth) * 0.35f;
            int x0 = cx + Math.round(Mth.cos(a0) * outer);
            int y0 = cy + Math.round(Mth.sin(a0) * outer);
            int x1 = cx + Math.round(Mth.cos(a1) * outer);
            int y1 = cy + Math.round(Mth.sin(a1) * outer);
            graphics.fill(Math.min(x0, x1) - 1, Math.min(y0, y1) - 1,
                    Math.max(x0, x1) + 1, Math.max(y0, y1) + 1, 0xFFC8C8D0);
        }
        fillCircle(graphics, cx, cy, Math.round(inner), 0xFFA8A8B8);
        fillCircle(graphics, cx, cy, Math.round(hub), 0xFF303038);
    }

    private static void fillCircle(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int span = (int) Math.sqrt(r * r - dy * dy);
            graphics.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, color);
        }
    }
}
