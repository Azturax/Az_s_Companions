package com.azscompanions.client.screen;

import com.azscompanions.menu.RadialCommandMenu;
import com.azscompanions.network.packet.RadialCommandPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Vanilla-ish pie menu for simple companion commands (hotkey or container bridge).
 */
public final class CompanionRadialScreen extends Screen {
    private enum Page { MAIN, EMOTE }

    private final int companionId;
    private Page page = Page.MAIN;
    private int hovered = -1;

    public CompanionRadialScreen(int companionId) {
        super(Component.translatable("screen.azscompanions.radial"));
        this.companionId = companionId;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Dim backdrop without locking the world pause.
        graphics.fill(0, 0, width, height, 0x66000000);

        int cx = width / 2;
        int cy = height / 2;
        int outer = 92;
        int inner = 28;

        Slice[] slices = currentSlices();
        hovered = sliceAt(mouseX, mouseY, cx, cy, inner, outer, slices.length);

        for (int i = 0; i < slices.length; i++) {
            drawSlice(graphics, cx, cy, inner, outer, i, slices.length, i == hovered, slices[i].label());
        }

        graphics.fill(cx - 22, cy - 22, cx + 22, cy + 22, 0xE0141C28);
        graphics.renderOutline(cx - 22, cy - 22, 44, 44, 0xFFF2A7C0);
        String center = page == Page.EMOTE ? "Emote" : "Kon";
        graphics.drawCenteredString(font, center, cx, cy - 4, 0xE8F4FF);

        if (hovered >= 0 && hovered < slices.length) {
            graphics.drawCenteredString(font, slices[hovered].label(), cx, cy + outer + 16, 0xFFFFFF);
        } else {
            graphics.drawCenteredString(font, Component.translatable("screen.azscompanions.radial.hint"),
                    cx, cy + outer + 16, 0xA0B0C0);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int cx = width / 2;
        int cy = height / 2;
        Slice[] slices = currentSlices();
        int idx = sliceAt((int) mouseX, (int) mouseY, cx, cy, 28, 92, slices.length);
        double dist = Math.hypot(mouseX - cx, mouseY - cy);
        if (dist < 28) {
            if (page == Page.EMOTE) {
                page = Page.MAIN;
                return true;
            }
            onClose();
            return true;
        }
        if (idx < 0) {
            return true;
        }
        Slice slice = slices[idx];
        if (slice.command() == null) {
            page = Page.EMOTE;
            return true;
        }
        PacketDistributor.sendToServer(new RadialCommandPacket(companionId, slice.command().name()));
        onClose();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private Slice[] currentSlices() {
        if (page == Page.EMOTE) {
            return new Slice[]{
                    new Slice(Component.translatable("screen.azscompanions.radial.wave"), RadialCommandMenu.Command.EMOTE_WAVE),
                    new Slice(Component.translatable("screen.azscompanions.radial.cheer"), RadialCommandMenu.Command.EMOTE_CHEER)
            };
        }
        return new Slice[]{
                new Slice(Component.translatable("screen.azscompanions.radial.follow"), RadialCommandMenu.Command.FOLLOW),
                new Slice(Component.translatable("screen.azscompanions.radial.stay"), RadialCommandMenu.Command.STAY),
                new Slice(Component.translatable("screen.azscompanions.radial.wander"), RadialCommandMenu.Command.WANDER),
                new Slice(Component.translatable("screen.azscompanions.radial.emote"), null)
        };
    }

    private static int sliceAt(int mouseX, int mouseY, int cx, int cy, int inner, int outer, int count) {
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double dist = Math.hypot(dx, dy);
        if (dist < inner || dist > outer + 8 || count <= 0) {
            return -1;
        }
        double angle = Math.atan2(dy, dx); // -PI..PI, 0 = east
        angle = (angle + Math.PI * 2.5d) % (Math.PI * 2.0d); // shift so first slice is north-ish
        double slice = (Math.PI * 2.0d) / count;
        return Mth.clamp((int) (angle / slice), 0, count - 1);
    }

    private void drawSlice(GuiGraphics graphics, int cx, int cy, int inner, int outer,
                           int index, int count, boolean highlight, Component label) {
        double slice = (Math.PI * 2.0d) / count;
        double start = -Math.PI * 0.5d + index * slice;
        double end = start + slice;
        int color = highlight ? 0xE0F2A7C0 : 0xC0283240;
        int segments = Math.max(8, (int) (24.0d / count));
        for (int s = 0; s < segments; s++) {
            double a0 = start + (end - start) * (s / (double) segments);
            double a1 = start + (end - start) * ((s + 1) / (double) segments);
            int x0 = cx + (int) (Math.cos(a0) * ((inner + outer) * 0.5d));
            int y0 = cy + (int) (Math.sin(a0) * ((inner + outer) * 0.5d));
            int x1 = cx + (int) (Math.cos(a1) * ((inner + outer) * 0.5d));
            int y1 = cy + (int) (Math.sin(a1) * ((inner + outer) * 0.5d));
            int midX = (x0 + x1) / 2;
            int midY = (y0 + y1) / 2;
            graphics.fill(midX - 14, midY - 10, midX + 14, midY + 10, color);
        }
        double mid = (start + end) * 0.5d;
        int lx = cx + (int) (Math.cos(mid) * ((inner + outer) * 0.55d));
        int ly = cy + (int) (Math.sin(mid) * ((inner + outer) * 0.55d));
        graphics.drawCenteredString(font, label, lx, ly - 4, highlight ? 0x1A1020 : 0xE8F4FF);
    }

    public static void openForOwnedCompanion() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        var companions = mc.level.getEntitiesOfClass(
                com.azscompanions.entity.CompanionEntity.class,
                mc.player.getBoundingBox().inflate(64.0d),
                c -> c.isAlive() && (c.isOwnedBy(mc.player) || c.isTrusted(mc.player)));
        if (companions.isEmpty()) {
            mc.player.displayClientMessage(Component.translatable("message.azscompanions.radial_no_companion"), true);
            return;
        }
        companions.sort((a, b) -> Double.compare(a.distanceToSqr(mc.player), b.distanceToSqr(mc.player)));
        mc.setScreen(new CompanionRadialScreen(companions.getFirst().getId()));
    }

    private record Slice(Component label, RadialCommandMenu.Command command) {
    }
}
