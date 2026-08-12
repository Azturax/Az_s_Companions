package com.azscompanions.client.screen;

import com.azscompanions.entity.FabricCompanionEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Read-only Companion | Owner info screen (Shift+RMB menu → Stats, or {@code /az stats}).
 */
public final class FabricCompanionStatsScreen extends Screen {
    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int SECTION_COLOR = 0xFFE0C080;
    private static final int VALUE_COLOR = 0xD0D0D0;

    private final FabricCompanionEntity companion;
    private final Screen parent;
    private final String whoAmI;
    private final String whatAmIDoing;
    private final String howWillIBe;
    private final int childCount;
    private final int ownedCount;
    private final String charmStatus;
    private final String aiStatus;

    private int panelX;
    private int panelY;
    private final int panelW = 360;
    private final int panelH = 228;

    public FabricCompanionStatsScreen(
            FabricCompanionEntity companion,
            Screen parent,
            String whoAmI,
            String whatAmIDoing,
            String howWillIBe,
            int childCount,
            int ownedCount,
            String charmStatus,
            String aiStatus
    ) {
        super(Component.translatable("screen.azscompanions.stats"));
        this.companion = companion;
        this.parent = parent;
        this.whoAmI = whoAmI == null ? "" : whoAmI;
        this.whatAmIDoing = whatAmIDoing == null ? "" : whatAmIDoing;
        this.howWillIBe = howWillIBe == null ? "" : howWillIBe;
        this.childCount = childCount;
        this.ownedCount = ownedCount;
        this.charmStatus = charmStatus == null || charmStatus.isBlank() ? "none" : charmStatus;
        this.aiStatus = aiStatus == null ? "" : aiStatus;
    }

    @Override
    protected void init() {
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
        }).bounds(panelX + (panelW - 120) / 2, panelY + panelH - 26, 120, 20).build());
    }

        public void renderBackground(GuiGraphics graphics) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.drawCenteredString(font, title, panelX + panelW / 2, panelY + 8, TITLE_COLOR);

        int midX = panelX + panelW / 2;
        graphics.fill(midX, panelY + 24, midX + 1, panelY + panelH - 32, 0xFF505050);

        int colW = (panelW / 2) - 18;
        int leftX = panelX + 10;
        int rightX = midX + 10;
        int y = panelY + 28;

        graphics.drawString(font, Component.translatable("screen.azscompanions.stats.section.companion"),
                leftX, y, SECTION_COLOR, false);
        graphics.drawString(font, Component.translatable("screen.azscompanions.stats.section.owner"),
                rightX, y, SECTION_COLOR, false);
        y += 12;

        int row = y;
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.name", companion.getDisplayName().getString());
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.form", companion.getForm().serializedName());
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.mode",
                companion.getMode().name().toLowerCase(java.util.Locale.ROOT));
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.attitude", companion.getAttitude().serializedName());
        String team = companion.getTeamId();
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.team",
                team == null || team.isBlank() ? "—" : team);
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.health",
                formatHp(companion.getHealth(), companion.getMaxHealth()));
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.radii",
                String.format("%.0f / %.0f / %.0f",
                        companion.getFollowRadius(), companion.getPersonalSpace(), companion.getWanderRadius()));
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.children",
                childCount + "/" + companion.getMaxChildren());
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.armor",
                companion.isArmorVisible()
                        ? Component.translatable("screen.azscompanions.stats.armor.show").getString()
                        : Component.translatable("screen.azscompanions.stats.armor.hide").getString());
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.persona.who", blankDash(whoAmI));
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.persona.what", blankDash(whatAmIDoing));
        row = line(graphics, leftX, row, colW, "screen.azscompanions.stats.persona.how", blankDash(howWillIBe));
        if (!aiStatus.isBlank()) {
            line(graphics, leftX, row, colW, "screen.azscompanions.stats.ai", aiStatus);
        }

        int ownerRow = y;
        Player player = minecraft != null ? minecraft.player : null;
        String playerName = player != null ? player.getGameProfile().getName() : "—";
        ownerRow = line(graphics, rightX, ownerRow, colW, "screen.azscompanions.stats.name", playerName);
        if (player != null) {
            ownerRow = line(graphics, rightX, ownerRow, colW, "screen.azscompanions.stats.health",
                    formatHp(player.getHealth(), player.getMaxHealth()));
            ownerRow = line(graphics, rightX, ownerRow, colW, "screen.azscompanions.stats.food",
                    String.valueOf(player.getFoodData().getFoodLevel()));
        }
        ownerRow = line(graphics, rightX, ownerRow, colW, "screen.azscompanions.stats.companions",
                String.valueOf(ownedCount));
        line(graphics, rightX, ownerRow, colW, "screen.azscompanions.stats.charm",
                Component.translatable("screen.azscompanions.stats.charm." + charmStatus).getString());

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int line(GuiGraphics graphics, int x, int y, int maxW, String labelKey, String value) {
        String label = Component.translatable(labelKey).getString();
        String text = label + ": " + (value == null ? "—" : value);
        graphics.drawString(font, font.plainSubstrByWidth(text, maxW), x, y, VALUE_COLOR, false);
        return y + 11;
    }

    private static String formatHp(float health, float max) {
        return String.format("%.0f/%.0f", health, max);
    }

    private static String blankDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
