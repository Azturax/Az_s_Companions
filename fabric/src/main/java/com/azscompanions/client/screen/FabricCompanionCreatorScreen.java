package com.azscompanions.client.screen;

import com.azscompanions.client.FabricClientAppearanceDraft;
import com.azscompanions.client.FabricCompanionSkinTextures;
import com.azscompanions.client.FabricPlayerSkinLookup;
import com.azscompanions.entity.CompanionBodyProportions;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.CompanionGender;
import com.azscompanions.network.FabricNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Companion character creator.
 * Layout: left category nav | centered preview | scrollable right-side controls.
 */
public final class FabricCompanionCreatorScreen extends Screen {
    public enum Category {
        NAME, SKIN, BODY
    }

    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;
    private static final int TEXT_LABEL = 0xFFFFFF;
    private static final int NAV_W = 96;
    private static final int PREVIEW_W = 120;
    private static final int GAP = 8;
    private static final int SCROLLBAR_W = 6;
    private static final int FOOTER_H = 30;
    private static final int HEADER_H = 34;

    private final FabricCompanionEntity companion;
    private final Screen parent;
    private final FabricClientAppearanceDraft draft;
    private Category category = Category.BODY;
    private EditBox nameBox;
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

    /** Right-column scroll state (pixels). */
    private int rightScroll;
    private int rightMaxScroll;
    private int rightViewTop;
    private int rightViewBottom;
    private int rightContentHeight;
    private final List<ScrollEntry> rightWidgets = new ArrayList<>();
    private final List<ScrollLabel> rightLabels = new ArrayList<>();
    /** Relative Y for live Mojang lookup status under the Name tab. */
    private int nameStatusContentY = -1;

    /** ~500ms debounce at 20 tps before Mojang username → skin lookup. */
    private static final int NAME_SKIN_DEBOUNCE_TICKS = 10;
    private int nameSkinDebounceTicks;
    private String pendingNameSkinLookup = "";
    private int nameSkinLookupGeneration;
    private String skinStatusMessage;

    public FabricCompanionCreatorScreen(FabricCompanionEntity companion, Screen parent) {
        super(Component.translatable("screen.azscompanions.creator"));
        this.companion = companion;
        this.parent = parent;
        this.draft = FabricClientAppearanceDraft.from(companion);
        FabricClientAppearanceDraft.ACTIVE = draft;
    }

    @Override
    protected void init() {
        // Fit within the screen at high GUI scale; keep a usable minimum.
        panelW = Math.min(500, Math.max(360, width - 16));
        panelH = Math.min(320, Math.max(180, height - 16));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        previewX = panelX + NAV_W + GAP + 4;
        previewY = panelY + HEADER_H + 4;
        previewH = Math.max(80, panelH - HEADER_H - FOOTER_H - 12);

        rightX = previewX + PREVIEW_W + GAP;
        rightW = Math.max(100, panelX + panelW - 14 - rightX - SCROLLBAR_W - 4);
        contentY = panelY + HEADER_H + 4;

        rightViewTop = contentY;
        rightViewBottom = panelY + panelH - FOOTER_H - 4;
        rightWidgets.clear();
        rightLabels.clear();
        nameStatusContentY = -1;

        clearWidgets();
        nameBox = null;
        slimButton = null;

        int catY = panelY + HEADER_H + 2;
        for (Category cat : Category.values()) {
            final Category c = cat;
            String label = (category == c ? "> " : "  ") + catLabel(cat);
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                syncEditBoxesToDraft();
                category = c;
                rightScroll = 0;
                init();
            }).bounds(panelX + 6, catY, NAV_W - 2, 20).build());
            catY += 24;
        }

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> cancelAndClose())
                .bounds(panelX + panelW - 144, panelY + panelH - 24, 64, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> commitAndClose())
                .bounds(panelX + panelW - 72, panelY + panelH - 24, 60, 18).build());

        int y = 0;
        if (category == Category.NAME) {
            addRightLabel(y, "Display name", 0xA0A0A0);
            y += 14;
            nameBox = new EditBox(font, rightX, 0, rightW, 18, Component.literal("Name"));
            nameBox.setMaxLength(32);
            nameBox.setValue(draft.name == null ? "" : draft.name);
            nameBox.setResponder(this::onNameEdited);
            addRightWidget(nameBox, y, 18);
            y += 24;
            y = addRightWrapped(y,
                    "Valid Minecraft names fetch that player's Mojang skin. \"Kon\" uses the Kon special skin.",
                    0xA0A0A0);
            y += 8;
            nameStatusContentY = y;
            y += 36; // reserve space for live lookup status
        } else if (category == Category.SKIN) {
            addRightLabel(y, "Mojang skin", 0xA0A0A0);
            y += 14;
            String path = draft.skinPath == null || draft.skinPath.isBlank() ? "(default)" : draft.skinPath;
            y = addRightWrapped(y, path, 0xFFFFFF);
            y += 8;
            slimButton = Button.builder(Component.literal(armLabel()), b -> {
                draft.slimArms = !draft.slimArms;
                if (slimButton != null) {
                    slimButton.setMessage(Component.literal(armLabel()));
                }
                pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_SLIM);
            }).bounds(rightX, 0, rightW, 20).build();
            addRightWidget(slimButton, y, 20);
            y += 28;
            y = addRightWrapped(y,
                    "Skins come from Mojang via the Name tab. Arm model updates from the profile when available.",
                    0xA0A0A0);
        } else if (category == Category.BODY) {
            addRightLabel(y, "Gender, size & proportions", 0xA0A0A0);
            y += 16;
            int half = (rightW - 6) / 2;
            Button female = Button.builder(
                    Component.literal(draft.gender == CompanionGender.FEMALE ? "[Female]" : "Female"),
                    b -> {
                        draft.gender = CompanionGender.FEMALE;
                        pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_GENDER | FabricNetworking.SettingsPayload.FLAG_PROPORTIONS);
                        init();
                    }).bounds(rightX, 0, half, 20).build();
            Button male = Button.builder(
                    Component.literal(draft.gender == CompanionGender.MALE ? "[Male]" : "Male"),
                    b -> {
                        draft.gender = CompanionGender.MALE;
                        pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_GENDER | FabricNetworking.SettingsPayload.FLAG_PROPORTIONS);
                        init();
                    }).bounds(rightX + half + 6, 0, half, 20).build();
            addRightWidget(female, y, 20);
            addRightWidget(male, y, 20);
            y += 26;
            y = addRightSlider(y, "Size", () -> draft.scale, v -> {
                        draft.scale = v;
                        pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_SCALE);
                    },
                    FabricCompanionEntity.MIN_BODY_SCALE, FabricCompanionEntity.MAX_BODY_SCALE);
            if (draft.gender.showsBust()) {
                y = addRightSlider(y, "Bust", () -> draft.bust, v -> {
                            draft.bust = v;
                            pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_BUST, CompanionBodyProportions.MAX_BUST);
                y = addRightSlider(y, "Waist", () -> draft.waist, v -> {
                            draft.waist = v;
                            pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_WAIST, CompanionBodyProportions.MAX_WAIST);
                y = addRightSlider(y, "Hips", () -> draft.hips, v -> {
                            draft.hips = v;
                            pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_HIPS, CompanionBodyProportions.MAX_HIPS);
                y = addRightSlider(y, "Shoulders", () -> draft.shoulders, v -> {
                            draft.shoulders = v;
                            pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_SHOULDERS, CompanionBodyProportions.MAX_SHOULDERS);
                y = addRightSlider(y, "Bust shape", () -> draft.bustOffset, v -> {
                            draft.bustOffset = v;
                            pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_BUST_OFFSET, CompanionBodyProportions.MAX_BUST_OFFSET);
            } else {
                y = addRightSlider(y, "Waist", () -> draft.waist, v -> {
                            draft.waist = v;
                            pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_WAIST, CompanionBodyProportions.MAX_WAIST);
                y = addRightSlider(y, "Hips", () -> draft.hips, v -> {
                            draft.hips = v;
                            pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_HIPS, CompanionBodyProportions.MAX_HIPS);
                y = addRightSlider(y, "Shoulders", () -> draft.shoulders, v -> {
                            draft.shoulders = v;
                            pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_SHOULDERS, CompanionBodyProportions.MAX_SHOULDERS);
            }
        }

        rightContentHeight = y + 4;
        int viewH = Math.max(1, rightViewBottom - rightViewTop);
        rightMaxScroll = Math.max(0, rightContentHeight - viewH);
        rightScroll = Mth.clamp(rightScroll, 0, rightMaxScroll);
        applyRightScroll();
    }

    private void addRightLabel(int contentYRel, String text, int color) {
        rightLabels.add(new ScrollLabel(contentYRel, Component.literal(text), color, false));
    }

    private int addRightWrapped(int contentYRel, String text, int color) {
        Component component = Component.literal(text);
        int lines = Math.max(1, font.split(component, rightW).size());
        rightLabels.add(new ScrollLabel(contentYRel, component, color, true));
        return contentYRel + lines * 10 + 2;
    }

    private void addRightWidget(AbstractWidget widget, int contentYRel, int height) {
        rightWidgets.add(new ScrollEntry(widget, contentYRel, height));
        addRenderableWidget(widget);
    }

    private int addRightSlider(int contentYRel, String label, Supplier<Float> getter, Consumer<Float> setter,
                               float min, float max) {
        double initial = Math.max(0.0d, Math.min(1.0d, (getter.get() - min) / (max - min)));
        AbstractSliderButton slider = new AbstractSliderButton(rightX, 0, rightW, 20, Component.empty(), initial) {
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
        };
        addRightWidget(slider, contentYRel, 20);
        return contentYRel + 26;
    }

    private void applyRightScroll() {
        rightScroll = Mth.clamp(rightScroll, 0, rightMaxScroll);
        for (ScrollEntry entry : rightWidgets) {
            int screenY = rightViewTop + entry.contentY - rightScroll;
            entry.widget.setY(screenY);
            boolean visible = screenY + entry.height > rightViewTop && screenY < rightViewBottom;
            entry.widget.visible = visible;
            entry.widget.active = visible;
        }
    }

    private boolean isOverRightPanel(double mouseX, double mouseY) {
        return mouseX >= rightX - 2
                && mouseX <= panelX + panelW - 6
                && mouseY >= rightViewTop
                && mouseY <= rightViewBottom;
    }

    private void drawScrollbar(GuiGraphics graphics) {
        if (rightMaxScroll <= 0) {
            return;
        }
        int trackX = panelX + panelW - 10;
        int trackTop = rightViewTop;
        int trackH = Math.max(1, rightViewBottom - rightViewTop);
        graphics.fill(trackX, trackTop, trackX + SCROLLBAR_W, trackTop + trackH, 0x66000000);

        int thumbH = Math.max(16, (int) (trackH * (trackH / (float) (trackH + rightMaxScroll))));
        int travel = trackH - thumbH;
        int thumbY = trackTop + (travel <= 0 ? 0 : (int) (travel * (rightScroll / (float) rightMaxScroll)));
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_W, thumbY + thumbH, 0xFFC0C0C0);
        graphics.fill(trackX, thumbY, trackX + SCROLLBAR_W, thumbY + 1, 0xFFFFFFFF);
    }

    private String armLabel() {
        return draft.slimArms ? "Arms: Slim" : "Arms: Wide";
    }

    private void syncEditBoxesToDraft() {
        if (nameBox != null) {
            draft.name = nameBox.getValue();
        }
    }

    private void onNameEdited(String value) {
        draft.name = value;
        pendingNameSkinLookup = value == null ? "" : value.trim();
        nameSkinDebounceTicks = NAME_SKIN_DEBOUNCE_TICKS;
        skinStatusMessage = null;
    }

    @Override
    public void tick() {
        super.tick();
        if (nameSkinDebounceTicks > 0) {
            nameSkinDebounceTicks--;
            if (nameSkinDebounceTicks == 0) {
                resolveNameToSkin(pendingNameSkinLookup);
            }
        }
    }

    private void resolveNameToSkin(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.equalsIgnoreCase("Kon")) {
            applyDraftSkin(FabricCompanionSkinTextures.DEFAULT_KON.toString(), draft.slimArms);
            skinStatusMessage = "Kon special skin";
            pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_NAME
                    | FabricNetworking.SettingsPayload.FLAG_SKIN
                    | FabricNetworking.SettingsPayload.FLAG_SLIM);
            return;
        }
        if (!FabricPlayerSkinLookup.isValidUsername(trimmed)) {
            skinStatusMessage = "Not a valid Minecraft username";
            pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_NAME);
            return;
        }
        final int generation = ++nameSkinLookupGeneration;
        final String requested = trimmed;
        skinStatusMessage = "Looking up " + requested + "…";
        FabricPlayerSkinLookup.lookupPlayerAsync(requested, opt -> {
            if (generation != nameSkinLookupGeneration) {
                return;
            }
            if (nameBox != null && !nameBox.getValue().trim().equalsIgnoreCase(requested)) {
                return;
            }
            if (draft.name == null || !draft.name.trim().equalsIgnoreCase(requested)) {
                return;
            }
            if (opt.isEmpty()) {
                skinStatusMessage = "No Mojang profile for \"" + requested + "\"";
                pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_NAME);
                return;
            }
            FabricPlayerSkinLookup.ResolvedPlayer player = opt.get();
            UUID uuid = player.uuid();
            skinStatusMessage = "Downloading skin for " + requested + "…";
            // Update name now; keep previous skin until the texture is fully registered.
            pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_NAME);
            FabricCompanionSkinTextures.loadPlayerSkin(uuid, ready -> {
                if (generation != nameSkinLookupGeneration) {
                    return;
                }
                if (nameBox != null && !nameBox.getValue().trim().equalsIgnoreCase(requested)) {
                    return;
                }
                if (draft.name == null || !draft.name.trim().equalsIgnoreCase(requested)) {
                    return;
                }
                if (ready.isEmpty()) {
                    skinStatusMessage = "Could not download skin for \"" + requested + "\"";
                    pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_NAME);
                    return;
                }
                FabricCompanionSkinTextures.ReadySkin skin = ready.get();
                applyDraftSkin("player:" + uuid, skin.slim());
                skinStatusMessage = "Skin: " + requested;
                pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_NAME
                        | FabricNetworking.SettingsPayload.FLAG_SKIN
                        | FabricNetworking.SettingsPayload.FLAG_SLIM);
            });
        });
    }

    private void applyDraftSkin(String skinPath, boolean slim) {
        draft.skinPath = skinPath;
        draft.slimArms = slim;
        if (slimButton != null) {
            slimButton.setMessage(Component.literal(armLabel()));
        }
    }

    private void pushLiveAppearance(int flags) {
        com.azscompanions.network.FabricNetworkingClient.sendSettings(new FabricNetworking.SettingsPayload(
                companion.getId(),
                draft.name == null ? "" : draft.name,
                draft.scale,
                draft.skinPath == null ? "" : draft.skinPath,
                draft.slimArms,
                draft.gender.isMale(),
                draft.bust,
                draft.waist,
                draft.hips,
                draft.shoulders,
                draft.bustOffset,
                flags
        ));
    }

    private static String catLabel(Category cat) {
        return switch (cat) {
            case NAME -> "Name";
            case SKIN -> "Face/Skin";
            case BODY -> "Body";
        };
    }

    private void commitAndClose() {
        syncEditBoxesToDraft();
        int flags = FabricNetworking.SettingsPayload.FLAG_NAME | FabricNetworking.SettingsPayload.FLAG_SCALE
                | FabricNetworking.SettingsPayload.FLAG_SKIN | FabricNetworking.SettingsPayload.FLAG_SLIM
                | FabricNetworking.SettingsPayload.FLAG_PROPORTIONS | FabricNetworking.SettingsPayload.FLAG_GENDER;
        com.azscompanions.network.FabricNetworkingClient.sendSettings(new FabricNetworking.SettingsPayload(
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
        FabricClientAppearanceDraft.ACTIVE = null;
        onClose();
    }

    private void cancelAndClose() {
        FabricClientAppearanceDraft.ACTIVE = null;
        onClose();
    }

    @Override
    public void onClose() {
        FabricClientAppearanceDraft.ACTIVE = null;
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (rightMaxScroll > 0 && isOverRightPanel(mouseX, mouseY)) {
            rightScroll = Mth.clamp(rightScroll - (int) (scrollY * 12), 0, rightMaxScroll);
            applyRightScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Clicking the scrollbar track jumps / starts drag via simple jump-to.
        if (button == 0 && rightMaxScroll > 0) {
            int trackX = panelX + panelW - 10;
            if (mouseX >= trackX && mouseX <= trackX + SCROLLBAR_W
                    && mouseY >= rightViewTop && mouseY <= rightViewBottom) {
                int trackH = Math.max(1, rightViewBottom - rightViewTop);
                float rel = (float) ((mouseY - rightViewTop) / trackH);
                rightScroll = Mth.clamp((int) (rel * rightMaxScroll), 0, rightMaxScroll);
                applyRightScroll();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.visible && nameBox.isFocused()
                && nameBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (nameBox != null && nameBox.visible && nameBox.isFocused()
                && nameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 3, 0x55FFFFFF);

        graphics.fill(panelX + NAV_W + 2, panelY + HEADER_H, panelX + NAV_W + 3, panelY + panelH - FOOTER_H, 0x40FFFFFF);
        graphics.fill(previewX + PREVIEW_W + 2, panelY + HEADER_H, previewX + PREVIEW_W + 3, panelY + panelH - FOOTER_H, 0x40FFFFFF);

        graphics.drawString(font, "Companion Customization", panelX + 10, panelY + 8, TEXT_LABEL, false);
        graphics.drawString(font, companion.getDisplayName().getString(), panelX + 10, panelY + 20, 0xA0A0A0, false);

        graphics.fill(previewX - 2, previewY - 2, previewX + PREVIEW_W + 2, previewY + previewH + 2, 0x66000000);
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics,
                    previewX, previewY,
                    previewX + PREVIEW_W, previewY + previewH,
                    (int) (48 * draft.scale / FabricCompanionEntity.DEFAULT_BODY_SCALE),
                    0.0625f, mouseX, mouseY, companion);
        } catch (Throwable ignored) {
            graphics.drawCenteredString(font, "Preview", previewX + PREVIEW_W / 2, previewY + previewH / 2, TEXT_LABEL);
        }

        // Hide scrollable controls from the default pass so we can clip them.
        for (ScrollEntry entry : rightWidgets) {
            entry.widget.visible = false;
        }
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.enableScissor(rightX - 2, rightViewTop, panelX + panelW - 12, rightViewBottom);
        for (ScrollLabel label : rightLabels) {
            int screenY = rightViewTop + label.contentY - rightScroll;
            if (screenY + 24 < rightViewTop || screenY > rightViewBottom) {
                continue;
            }
            if (label.wrapped) {
                graphics.drawWordWrap(font, label.text, rightX, screenY, rightW, label.color);
            } else {
                graphics.drawString(font, label.text.getString(), rightX, screenY, label.color, false);
            }
        }
        if (category == Category.NAME && nameStatusContentY >= 0
                && skinStatusMessage != null && !skinStatusMessage.isBlank()) {
            int statusY = rightViewTop + nameStatusContentY - rightScroll;
            graphics.drawWordWrap(font, Component.literal(skinStatusMessage), rightX, statusY, rightW, 0xFFE0A0);
        }
        for (ScrollEntry entry : rightWidgets) {
            int screenY = rightViewTop + entry.contentY - rightScroll;
            boolean visible = screenY + entry.height > rightViewTop && screenY < rightViewBottom;
            entry.widget.visible = visible;
            entry.widget.active = visible;
            if (visible) {
                entry.widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        graphics.disableScissor();

        drawScrollbar(graphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ScrollEntry(AbstractWidget widget, int contentY, int height) {
    }

    private record ScrollLabel(int contentY, Component text, int color, boolean wrapped) {
    }
}
