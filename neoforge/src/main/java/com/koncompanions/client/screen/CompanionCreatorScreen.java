package com.koncompanions.client.screen;

import com.koncompanions.client.ClientAppearanceDraft;
import com.koncompanions.client.CompanionSkinTextures;
import com.koncompanions.client.SkinImportService;
import com.koncompanions.entity.CompanionBodyProportions;
import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionGender;
import com.koncompanions.network.packet.CompanionSettingsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Companion character creator.
 * Layout: left category nav | centered preview | right-side controls (sliders / skin / name).
 */
public final class CompanionCreatorScreen extends Screen {
    public enum Category {
        NAME, SKIN, BODY, CONFIRM
    }

    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;
    private static final int TEXT_LABEL = 0xFFFFFF;
    private static final int NAV_W = 96;
    private static final int PREVIEW_W = 130;
    private static final int GAP = 10;

    private final CompanionEntity companion;
    @Nullable
    private final Screen parent;
    private final ClientAppearanceDraft draft;
    private Category category = Category.BODY;
    private EditBox nameBox;
    private EditBox skinBox;
    private Button slimButton;
    private int panelX;
    private int panelY;
    private int panelW = 460;
    private int panelH = 260;
    private int previewX;
    private int previewY;
    private int previewH = 176;
    private int rightX;
    private int rightW;
    private int contentY;
    private int skinIndex = -1;

    public CompanionCreatorScreen(CompanionEntity companion, @Nullable Screen parent) {
        super(Component.translatable("screen.koncompanions.creator"));
        this.companion = companion;
        this.parent = parent;
        this.draft = ClientAppearanceDraft.from(companion);
        ClientAppearanceDraft.ACTIVE = draft;
    }

    @Override
    protected void init() {
        panelW = Math.min(480, Math.max(420, width - 24));
        panelH = Math.min(280, Math.max(240, height - 24));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        previewX = panelX + NAV_W + GAP + 8;
        previewY = panelY + 36;
        previewH = panelH - 70;

        rightX = previewX + PREVIEW_W + GAP;
        rightW = panelX + panelW - 12 - rightX;
        contentY = panelY + 40;

        clearWidgets();
        nameBox = null;
        skinBox = null;
        slimButton = null;

        int catY = panelY + 36;
        for (Category cat : Category.values()) {
            final Category c = cat;
            String label = (category == c ? "> " : "  ") + catLabel(cat);
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                syncEditBoxesToDraft();
                category = c;
                init();
            }).bounds(panelX + 8, catY, NAV_W - 4, 20).build());
            catY += 24;
        }

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> cancelAndClose())
                .bounds(panelX + panelW - 144, panelY + panelH - 26, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> commitAndClose())
                .bounds(panelX + panelW - 72, panelY + panelH - 26, 60, 20).build());

        if (category == Category.NAME) {
            nameBox = new EditBox(font, rightX, contentY + 16, rightW, 18, Component.literal("Name"));
            nameBox.setMaxLength(32);
            nameBox.setValue(draft.name == null ? "" : draft.name);
            nameBox.setResponder(s -> draft.name = s);
            addRenderableWidget(nameBox);
        } else if (category == Category.SKIN) {
            skinBox = new EditBox(font, rightX, contentY + 16, rightW, 18, Component.literal("Skin"));
            skinBox.setMaxLength(256);
            skinBox.setValue(draft.skinPath == null ? "" : draft.skinPath);
            skinBox.setResponder(s -> draft.skinPath = s);
            addRenderableWidget(skinBox);

            int row = contentY + 42;
            int half = (rightW - 6) / 2;
            addRenderableWidget(Button.builder(Component.literal("Import…"), b -> openFileImport())
                    .bounds(rightX, row, half, 20).build());
            slimButton = Button.builder(Component.literal(armLabel()), b -> {
                draft.slimArms = !draft.slimArms;
                if (slimButton != null) {
                    slimButton.setMessage(Component.literal(armLabel()));
                }
            }).bounds(rightX + half + 6, row, half, 20).build();
            addRenderableWidget(slimButton);

            row += 26;
            addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleLocalSkin(-1))
                    .bounds(rightX, row, 28, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleLocalSkin(1))
                    .bounds(rightX + 32, row, 28, 20).build());
        } else if (category == Category.BODY) {
            int y = contentY + 8;
            int half = (rightW - 6) / 2;
            addRenderableWidget(Button.builder(
                    Component.literal(draft.gender == CompanionGender.FEMALE ? "[Female]" : "Female"),
                    b -> {
                        draft.gender = CompanionGender.FEMALE;
                        init();
                    }).bounds(rightX, y, half, 20).build());
            addRenderableWidget(Button.builder(
                    Component.literal(draft.gender == CompanionGender.MALE ? "[Male]" : "Male"),
                    b -> {
                        draft.gender = CompanionGender.MALE;
                        init();
                    }).bounds(rightX + half + 6, y, half, 20).build());

            y += 26;
            addSlider("Size", y, () -> draft.scale, v -> draft.scale = v,
                    CompanionEntity.MIN_BODY_SCALE, CompanionEntity.MAX_BODY_SCALE);
            if (draft.gender.showsBust()) {
                addSlider("Bust", y + 26, () -> draft.bust, v -> draft.bust = v,
                        CompanionBodyProportions.MIN_BUST, CompanionBodyProportions.MAX_BUST);
                addSlider("Waist", y + 52, () -> draft.waist, v -> draft.waist = v,
                        CompanionBodyProportions.MIN_WAIST, CompanionBodyProportions.MAX_WAIST);
                addSlider("Hips", y + 78, () -> draft.hips, v -> draft.hips = v,
                        CompanionBodyProportions.MIN_HIPS, CompanionBodyProportions.MAX_HIPS);
                addSlider("Shoulders", y + 104, () -> draft.shoulders, v -> draft.shoulders = v,
                        CompanionBodyProportions.MIN_SHOULDERS, CompanionBodyProportions.MAX_SHOULDERS);
                addSlider("Bust shape", y + 130, () -> draft.bustOffset, v -> draft.bustOffset = v,
                        CompanionBodyProportions.MIN_BUST_OFFSET, CompanionBodyProportions.MAX_BUST_OFFSET);
            } else {
                addSlider("Waist", y + 26, () -> draft.waist, v -> draft.waist = v,
                        CompanionBodyProportions.MIN_WAIST, CompanionBodyProportions.MAX_WAIST);
                addSlider("Hips", y + 52, () -> draft.hips, v -> draft.hips = v,
                        CompanionBodyProportions.MIN_HIPS, CompanionBodyProportions.MAX_HIPS);
                addSlider("Shoulders", y + 78, () -> draft.shoulders, v -> draft.shoulders = v,
                        CompanionBodyProportions.MIN_SHOULDERS, CompanionBodyProportions.MAX_SHOULDERS);
            }
        } else if (category == Category.CONFIRM) {
            addRenderableWidget(Button.builder(Component.literal("Save Appearance"), b -> commitAndClose())
                    .bounds(rightX, contentY + 24, rightW, 22).build());
        }
    }

    private String armLabel() {
        return draft.slimArms ? "Arms: Slim" : "Arms: Wide";
    }

    private void addSlider(String label, int y, Supplier<Float> getter, Consumer<Float> setter, float min, float max) {
        double initial = Math.max(0.0d, Math.min(1.0d, (getter.get() - min) / (max - min)));
        addRenderableWidget(new AbstractSliderButton(rightX, y, rightW, 20, Component.empty(), initial) {
            {
                updateMessage();
            }

            @Override
            protected void updateMessage() {
                setMessage(Component.literal(String.format(Locale.ROOT, "%s: %.2f", label, getter.get())));
            }

            @Override
            protected void applyValue() {
                float v = min + (float) (this.value * (max - min));
                float step = "Size".equals(label) ? 0.1f : 0.01f;
                v = Math.round(v / step) * step;
                setter.accept(Math.max(min, Math.min(max, v)));
                updateMessage();
            }
        });
    }

    private void syncEditBoxesToDraft() {
        if (nameBox != null) {
            draft.name = nameBox.getValue();
        }
        if (skinBox != null) {
            draft.skinPath = skinBox.getValue();
        }
    }

    private void openFileImport() {
        if (minecraft == null) {
            return;
        }
        SkinImportService.pickPngAsync(opt -> opt.ifPresent(source ->
                SkinImportService.importLocalPng(minecraft.gameDirectory.toPath(), source, "companion_" + companion.getId())
                        .ifPresent(dest -> {
                            String file = dest.getFileName().toString();
                            CompanionSkinTextures.invalidate(file);
                            draft.skinPath = "local:" + file;
                            if (skinBox != null) {
                                skinBox.setResponder(null);
                                skinBox.setValue(draft.skinPath);
                                skinBox.setResponder(s -> draft.skinPath = s);
                            }
                        })));
    }

    private void cycleLocalSkin(int delta) {
        if (minecraft == null) {
            return;
        }
        List<String> skins = SkinImportService.listLocalSkinFiles(minecraft.gameDirectory.toPath());
        if (skins.isEmpty()) {
            return;
        }
        String current = draft.skinPath == null ? "" : draft.skinPath;
        if (current.startsWith("local:")) {
            skinIndex = skins.indexOf(current.substring("local:".length()));
        }
        if (skinIndex < 0) {
            skinIndex = 0;
        } else {
            skinIndex = Math.floorMod(skinIndex + delta, skins.size());
        }
        String file = skins.get(skinIndex);
        CompanionSkinTextures.invalidate(file);
        draft.skinPath = "local:" + file;
        if (skinBox != null) {
            skinBox.setResponder(null);
            skinBox.setValue(draft.skinPath);
            skinBox.setResponder(s -> draft.skinPath = s);
        }
    }

    private static String catLabel(Category cat) {
        return switch (cat) {
            case NAME -> "Name";
            case SKIN -> "Face/Skin";
            case BODY -> "Body";
            case CONFIRM -> "Confirm";
        };
    }

    private void commitAndClose() {
        syncEditBoxesToDraft();
        int flags = CompanionSettingsPacket.FLAG_NAME | CompanionSettingsPacket.FLAG_SCALE
                | CompanionSettingsPacket.FLAG_SKIN | CompanionSettingsPacket.FLAG_SLIM
                | CompanionSettingsPacket.FLAG_PROPORTIONS | CompanionSettingsPacket.FLAG_GENDER;
        PacketDistributor.sendToServer(new CompanionSettingsPacket(
                companion.getId(),
                draft.name,
                draft.scale,
                draft.skinPath,
                draft.slimArms,
                draft.gender.isMale(),
                draft.bust,
                draft.waist,
                draft.hips,
                draft.shoulders,
                draft.bustOffset,
                flags
        ));
        ClientAppearanceDraft.ACTIVE = null;
        onClose();
    }

    private void cancelAndClose() {
        ClientAppearanceDraft.ACTIVE = null;
        onClose();
    }

    @Override
    public void onClose() {
        ClientAppearanceDraft.ACTIVE = null;
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.isFocused() && nameBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (skinBox != null && skinBox.isFocused() && skinBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (nameBox != null && nameBox.isFocused() && nameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (skinBox != null && skinBox.isFocused() && skinBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 3, 0x55FFFFFF);

        // Column guides
        graphics.fill(panelX + NAV_W + 4, panelY + 32, panelX + NAV_W + 5, panelY + panelH - 32, 0x40FFFFFF);
        graphics.fill(previewX + PREVIEW_W + 4, panelY + 32, previewX + PREVIEW_W + 5, panelY + panelH - 32, 0x40FFFFFF);

        graphics.drawString(font, "Companion Customization", panelX + 10, panelY + 8, TEXT_LABEL, false);
        graphics.drawString(font, companion.getDisplayName().getString(), panelX + 10, panelY + 20, 0xA0A0A0, false);

        graphics.fill(previewX - 2, previewY - 2, previewX + PREVIEW_W + 2, previewY + previewH + 2, 0x66000000);
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    previewX, previewY,
                    previewX + PREVIEW_W, previewY + previewH,
                    (int) (48 * draft.scale / CompanionEntity.DEFAULT_BODY_SCALE),
                    0.0625f, mouseX, mouseY, companion);
        } catch (Throwable ignored) {
            graphics.drawCenteredString(font, "Preview", previewX + PREVIEW_W / 2, previewY + previewH / 2, TEXT_LABEL);
        }

        if (category == Category.NAME) {
            graphics.drawString(font, "Display name", rightX, contentY, 0xA0A0A0, false);
        } else if (category == Category.BODY) {
            graphics.drawString(font, "Gender, size & proportions", rightX, contentY - 12, 0xA0A0A0, false);
        } else if (category == Category.SKIN) {
            graphics.drawString(font, "Skin path / import", rightX, contentY, 0xA0A0A0, false);
            graphics.drawWordWrap(font,
                    Component.literal("Import a 64x64 PNG, or cycle local skins with < >."),
                    rightX, contentY + 96, rightW, 0xA0A0A0);
        } else if (category == Category.CONFIRM) {
            graphics.drawWordWrap(font, Component.literal("Save applies name, skin, gender, and body proportions."),
                    rightX, contentY, rightW, 0xA0A0A0);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
