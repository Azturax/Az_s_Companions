package com.azscompanions.client.hud;

import com.azscompanions.teamfight.ClientTeamFightHud;
import com.azscompanions.teamfight.TeamFightHudSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Streamer scoreboard: Team Left | Team Right with scores, bits, members, price table, tops.
 */
public final class TeamFightHudOverlay {
    private TeamFightHudOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics, float partialTick) {
        if (!ClientTeamFightHud.shouldRender()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null) {
            return;
        }
        TeamFightHudSnapshot s = ClientTeamFightHud.get();
        Font font = mc.font;
        int sw = mc.getWindow().getGuiScaledWidth();
        int top = 8;
        int leftX = 8;
        int rightX = sw - 160;

        fillPanel(graphics, leftX - 2, top - 2, 150, 72);
        fillPanel(graphics, rightX - 2, top - 2, 150, 72);

        graphics.text(font, "TEAM " + s.teamLeft().toUpperCase(), leftX, top, 0xFF5555, true);
        graphics.text(font, "Score " + s.scoreLeft() + "  Bits " + s.bitsLeft(), leftX, top + 10, 0xFFFFFF, true);
        graphics.text(font, trim(s.membersLeft(), 22), leftX, top + 20, 0xCCCCCC, true);

        graphics.text(font, "TEAM " + s.teamRight().toUpperCase(), rightX, top, 0x5555FF, true);
        graphics.text(font, "Score " + s.scoreRight() + "  Bits " + s.bitsRight(), rightX, top + 10, 0xFFFFFF, true);
        graphics.text(font, trim(s.membersRight(), 22), rightX, top + 20, 0xCCCCCC, true);

        String prices = "Tiers: " + trim(s.priceTable(), 48);
        graphics.text(font, prices, sw / 2 - font.width(prices) / 2, top + 40, 0xFFD700, true);
        String tops = "Top bits: " + trim(s.topBits(), 40);
        graphics.text(font, tops, sw / 2 - font.width(tops) / 2, top + 50, 0xAAAAAA, true);
        String kills = "Top kills: " + trim(s.topKills(), 40);
        graphics.text(font, kills, sw / 2 - font.width(kills) / 2, top + 60, 0xAAAAAA, true);
    }

    private static void fillPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0x66000000);
    }

    private static String trim(String s, int max) {
        if (s == null || s.isBlank()) {
            return "-";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
