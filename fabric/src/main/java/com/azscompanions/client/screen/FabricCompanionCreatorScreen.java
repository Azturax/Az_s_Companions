package com.azscompanions.client.screen;

import com.azscompanions.client.FabricClientAppearanceDraft;
import com.azscompanions.client.FabricCompanionSkinTextures;
import com.azscompanions.client.FabricPlayerSkinLookup;
import com.azscompanions.entity.CompanionBodyProportions;
import com.azscompanions.entity.CompanionContextSkinSupport;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionGender;
import com.azscompanions.entity.CompanionOrbSettings;
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
    public enum TopTab {
        APPEARANCE, ACTIVITY
    }

    public enum Category {
        NAME, FORM, SKIN, BODY
    }

    private static final int PANEL_BG = 0xC0101010;
    private static final int PANEL_EDGE = 0xFF8B8B8B;
    private static final int TEXT_LABEL = 0xFFFFFF;
    private static final int NAV_W = 96;
    private static final int PREVIEW_W = 120;
    private static final int GAP = 8;
    private static final int SCROLLBAR_W = 6;
    private static final int FOOTER_H = 30;
    private static final int HEADER_H = 52;

    private final FabricCompanionEntity companion;
    private final Screen parent;
    private final FabricClientAppearanceDraft draft;
    private TopTab topTab = TopTab.APPEARANCE;
    private Category category = Category.BODY;
    private EditBox nameBox;
    private EditBox sleepLocalBox;
    private EditBox sleepUrlBox;
    private EditBox bathLocalBox;
    private EditBox bathUrlBox;
    private EditBox adventureLocalBox;
    private EditBox adventureUrlBox;
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
    private boolean draggingScrollbar;
    private int scrollbarDragGrab;
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
        sleepLocalBox = sleepUrlBox = bathLocalBox = bathUrlBox = adventureLocalBox = adventureUrlBox = null;

        int tabY = panelY + 6;
        int tabW = 110;
        addRenderableWidget(Button.builder(
                Component.literal(topTab == TopTab.APPEARANCE ? "[Appearance]" : "Appearance"),
                b -> {
                    syncEditBoxesToDraft();
                    topTab = TopTab.APPEARANCE;
                    rightScroll = 0;
                    init();
                }).bounds(panelX + panelW - tabW * 2 - 14, tabY, tabW, 18).build());
        addRenderableWidget(Button.builder(
                Component.literal(topTab == TopTab.ACTIVITY ? "[Activity]" : "Activity"),
                b -> {
                    syncEditBoxesToDraft();
                    topTab = TopTab.ACTIVITY;
                    rightScroll = 0;
                    init();
                }).bounds(panelX + panelW - tabW - 8, tabY, tabW, 18).build());

        if (topTab == TopTab.APPEARANCE) {
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
        }

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> cancelAndClose())
                .bounds(panelX + panelW - 144, panelY + panelH - 24, 64, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> commitAndClose())
                .bounds(panelX + panelW - 72, panelY + panelH - 24, 60, 18).build());

        int y = 0;
        if (topTab == TopTab.ACTIVITY) {
            y = buildActivityTab(y);
        } else if (category == Category.NAME) {
            addRightLabel(y, "Display name", 0xA0A0A0);
            y += 14;
            nameBox = new EditBox(font, rightX, 0, rightW, 18, Component.literal("Name"));
            nameBox.setMaxLength(32);
            nameBox.setValue(draft.name == null ? "" : draft.name);
            nameBox.setResponder(this::onNameEdited);
            addRightWidget(nameBox, y, 18);
            y += 24;
            y = addRightWrapped(y,
                    "Valid Minecraft usernames fetch that player's Mojang skin.",
                    0xA0A0A0);
            y += 8;
            Button nameTagBtn = Button.builder(
                    Component.literal(draft.showNameTag ? "Nametag: Show" : "Nametag: Hide"),
                    b -> {
                        draft.showNameTag = !draft.showNameTag;
                        b.setMessage(Component.literal(draft.showNameTag ? "Nametag: Show" : "Nametag: Hide"));
                        pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_SHOW_NAME);
                    }).bounds(rightX, 0, rightW, 20).build();
            addRightWidget(nameTagBtn, y, 20);
            y += 28;
            Button armorBtn = Button.builder(
                    Component.literal(draft.showArmor ? "Armor: Show" : "Armor: Hide"),
                    b -> {
                        draft.showArmor = !draft.showArmor;
                        b.setMessage(Component.literal(draft.showArmor ? "Armor: Show" : "Armor: Hide"));
                        pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_SHOW_ARMOR);
                    }).bounds(rightX, 0, rightW, 20).build();
            addRightWidget(armorBtn, y, 20);
            y += 28;
            nameStatusContentY = y;
            y += 36; // reserve space for live lookup status
        } else if (category == Category.FORM) {
            addRightLabel(y, "Companion form", 0xA0A0A0);
            y += 14;
            y = addRightWrapped(y, "Current: " + draft.form.displayLabel(), 0xFFFFFF);
            y += 8;
            if (!draft.form.isPlayer()) {
                addRightLabel(y, "Display name", 0xA0A0A0);
                y += 14;
                nameBox = new EditBox(font, rightX, 0, rightW, 18, Component.literal("Name"));
                nameBox.setMaxLength(32);
                nameBox.setValue(draft.name == null ? "" : draft.name);
                nameBox.setResponder(this::onFormNameEdited);
                addRightWidget(nameBox, y, 18);
                y += 24;
                y = addRightWrapped(y, "Custom name for this mob companion (same as Name tab).", 0xA0A0A0);
                y += 8;
            }
            y = addFormGroupButtons(y, "Player", CompanionForm.FormGroup.PLAYER);
            y = addFormGroupButtons(y, "Animals", CompanionForm.FormGroup.ANIMAL);
            y = addFormGroupButtons(y, "Hostiles", CompanionForm.FormGroup.HOSTILE);
            y = addFormGroupButtons(y, "Special", CompanionForm.FormGroup.SPECIAL);
            if (draft.form != null && draft.form.isOrb()) {
                y = addOrbSettingsSection(y);
            }
            y = addRightWrapped(y,
                    "Forms keep ownership, charm, Follow/Stay/Wander, and CCI actions. Creeper excluded.",
                    0xA0A0A0);
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
        syncContextBoxesToDraft();
    }

    private void syncContextBoxesToDraft() {
        if (sleepLocalBox != null && sleepUrlBox != null
                && (!sleepLocalBox.getValue().isBlank() || !sleepUrlBox.getValue().isBlank())) {
            draft.sleepingSkinPath = preferContextPath(sleepLocalBox.getValue(), sleepUrlBox.getValue());
        }
        if (bathLocalBox != null && bathUrlBox != null
                && (!bathLocalBox.getValue().isBlank() || !bathUrlBox.getValue().isBlank())) {
            draft.bathingSkinPath = preferContextPath(bathLocalBox.getValue(), bathUrlBox.getValue());
        }
        if (adventureLocalBox != null && adventureUrlBox != null
                && (!adventureLocalBox.getValue().isBlank() || !adventureUrlBox.getValue().isBlank())) {
            draft.adventuringSkinPath = preferContextPath(adventureLocalBox.getValue(), adventureUrlBox.getValue());
        }
    }

    private void onNameEdited(String value) {
        draft.name = value;
        pendingNameSkinLookup = value == null ? "" : value.trim();
        nameSkinDebounceTicks = NAME_SKIN_DEBOUNCE_TICKS;
        skinStatusMessage = null;
    }

    /** Form-tab name field: persist custom name without Mojang skin lookup. */
    private void onFormNameEdited(String value) {
        draft.name = value;
        pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_NAME);
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
            skinStatusMessage = null;
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

    private int buildActivityTab(int y) {
        boolean playerForm = draft.form != null && draft.form.isPlayer();
        addRightLabel(y, "Activity outfits", 0xA0A0A0);
        y += 14;
        y = addRightWrapped(y,
                "Player form only. Priority: context outfit → custom skin → default.",
                playerForm ? 0xA0A0A0 : 0xFFAA66);
        y += 6;
        if (!playerForm) {
            y = addRightWrapped(y,
                    "Switch Form → Player to apply these skins in-game. Settings still save.",
                    0xFFAA66);
            y += 8;
        }
        y = addContextSkinSection(y, "Sleeping",
                draft.sleepingSkinPath,
                (local, url) -> {
                    draft.sleepingSkinPath = preferContextPath(local, url);
                    pushContextSkins();
                },
                () -> {
                    draft.sleepingSkinPath = "";
                    pushContextSkins();
                    init();
                },
                boxes -> {
                    sleepLocalBox = boxes[0];
                    sleepUrlBox = boxes[1];
                });
        y = addContextSkinSection(y, "Bathing",
                draft.bathingSkinPath,
                (local, url) -> {
                    draft.bathingSkinPath = preferContextPath(local, url);
                    pushContextSkins();
                },
                () -> {
                    draft.bathingSkinPath = "";
                    pushContextSkins();
                    init();
                },
                boxes -> {
                    bathLocalBox = boxes[0];
                    bathUrlBox = boxes[1];
                });
        y = addContextSkinSection(y, "Adventuring",
                draft.adventuringSkinPath,
                (local, url) -> {
                    draft.adventuringSkinPath = preferContextPath(local, url);
                    pushContextSkins();
                },
                () -> {
                    draft.adventuringSkinPath = "";
                    pushContextSkins();
                    init();
                },
                boxes -> {
                    adventureLocalBox = boxes[0];
                    adventureUrlBox = boxes[1];
                });
        y = addRightWrapped(y,
                "Local: file under config/azscompanions/skins/ (local:name.png). URL: https://…",
                0x808080);
        y += 4;
        y = addRightWrapped(y,
                "Applies when sleeping in a bed, in water (bathing), or exploring with you.",
                0x808080);
        return y;
    }

    @FunctionalInterface
    private interface ContextApply {
        void apply(String local, String url);
    }

    private int addContextSkinSection(
            int y,
            String title,
            String current,
            ContextApply onApply,
            Runnable onClear,
            Consumer<EditBox[]> boxSink
    ) {
        addRightLabel(y, title, 0xFFFFFF);
        y += 12;
        String shown = current == null || current.isBlank() ? "(none — uses custom/default)" : current;
        y = addRightWrapped(y, shown, 0xC0C0C0);
        y += 4;
        addRightLabel(y, "Local path", 0xA0A0A0);
        y += 12;
        EditBox local = new EditBox(font, rightX, 0, rightW, 18, Component.literal(title + " local"));
        local.setMaxLength(CompanionContextSkinSupport.MAX_PATH_LENGTH);
        if (CompanionContextSkinSupport.isLocalSkin(current)) {
            local.setValue(current);
        }
        addRightWidget(local, y, 18);
        y += 22;
        addRightLabel(y, "URL", 0xA0A0A0);
        y += 12;
        EditBox url = new EditBox(font, rightX, 0, rightW, 18, Component.literal(title + " url"));
        url.setMaxLength(CompanionContextSkinSupport.MAX_PATH_LENGTH);
        if (CompanionContextSkinSupport.isUrlSkin(current)) {
            url.setValue(current.startsWith("url:") ? current.substring(4) : current);
        } else if (current != null && !current.isBlank()
                && !CompanionContextSkinSupport.isLocalSkin(current)
                && !current.startsWith("player:")) {
            url.setValue(current);
        }
        addRightWidget(url, y, 18);
        y += 22;
        boxSink.accept(new EditBox[]{local, url});
        int half = (rightW - 6) / 2;
        Button apply = Button.builder(Component.literal("Apply"), b -> {
            syncEditBoxesToDraft();
            onApply.apply(local.getValue(), url.getValue());
            init();
        }).bounds(rightX, 0, half, 18).build();
        Button clear = Button.builder(Component.literal("Clear"), b -> onClear.run())
                .bounds(rightX + half + 6, 0, half, 18).build();
        addRightWidget(apply, y, 18);
        addRightWidget(clear, y, 18);
        return y + 28;
    }

    private static String preferContextPath(String local, String url) {
        String u = url == null ? "" : url.trim();
        if (!u.isEmpty()) {
            return CompanionContextSkinSupport.sanitize(u);
        }
        String l = local == null ? "" : local.trim();
        if (l.isEmpty()) {
            return "";
        }
        if (!l.toLowerCase(Locale.ROOT).startsWith("local:")) {
            l = "local:" + l;
        }
        return CompanionContextSkinSupport.sanitize(l);
    }

    private void pushContextSkins() {
        com.azscompanions.network.FabricNetworkingClient.sendContextSkins(new FabricNetworking.ContextSkinsPayload(
                companion.getId(),
                draft.sleepingSkinPath == null ? "" : draft.sleepingSkinPath,
                draft.bathingSkinPath == null ? "" : draft.bathingSkinPath,
                draft.adventuringSkinPath == null ? "" : draft.adventuringSkinPath
        ));
    }

    private int addOrbSettingsSection(int y) {
        addRightLabel(y, "Glowing Orb", 0xA0A0A0);
        y += 14;
        y = addRightSlider(y, "Red",
                () -> (float) CompanionOrbSettings.red(draft.orbColorRgb),
                v -> {
                    draft.orbColorRgb = CompanionOrbSettings.rgb(
                            Math.round(v),
                            CompanionOrbSettings.green(draft.orbColorRgb),
                            CompanionOrbSettings.blue(draft.orbColorRgb));
                    pushOrbSettings();
                },
                CompanionOrbSettings.MIN_CHANNEL, CompanionOrbSettings.MAX_CHANNEL);
        y = addRightSlider(y, "Green",
                () -> (float) CompanionOrbSettings.green(draft.orbColorRgb),
                v -> {
                    draft.orbColorRgb = CompanionOrbSettings.rgb(
                            CompanionOrbSettings.red(draft.orbColorRgb),
                            Math.round(v),
                            CompanionOrbSettings.blue(draft.orbColorRgb));
                    pushOrbSettings();
                },
                CompanionOrbSettings.MIN_CHANNEL, CompanionOrbSettings.MAX_CHANNEL);
        y = addRightSlider(y, "Blue",
                () -> (float) CompanionOrbSettings.blue(draft.orbColorRgb),
                v -> {
                    draft.orbColorRgb = CompanionOrbSettings.rgb(
                            CompanionOrbSettings.red(draft.orbColorRgb),
                            CompanionOrbSettings.green(draft.orbColorRgb),
                            Math.round(v));
                    pushOrbSettings();
                },
                CompanionOrbSettings.MIN_CHANNEL, CompanionOrbSettings.MAX_CHANNEL);
        y = addRightSlider(y, "Brightness",
                () -> (float) draft.orbBrightness,
                v -> {
                    draft.orbBrightness = Math.round(v);
                    pushOrbSettings();
                },
                CompanionOrbSettings.MIN_BRIGHTNESS, CompanionOrbSettings.MAX_BRIGHTNESS);
        y = addRightSlider(y, "Float height",
                () -> draft.orbFloatHeight,
                v -> {
                    draft.orbFloatHeight = v;
                    pushOrbSettings();
                },
                CompanionOrbSettings.MIN_FLOAT_HEIGHT, CompanionOrbSettings.MAX_FLOAT_HEIGHT);
        y = addRightSlider(y, "Float bob",
                () -> draft.orbFloatAmplitude,
                v -> {
                    draft.orbFloatAmplitude = v;
                    pushOrbSettings();
                },
                CompanionOrbSettings.MIN_FLOAT_AMPLITUDE, CompanionOrbSettings.MAX_FLOAT_AMPLITUDE);
        y = addRightSlider(y, "Float speed",
                () -> draft.orbFloatSpeed,
                v -> {
                    draft.orbFloatSpeed = v;
                    pushOrbSettings();
                },
                CompanionOrbSettings.MIN_FLOAT_SPEED, CompanionOrbSettings.MAX_FLOAT_SPEED);
        y = addRightSlider(y, "Offset X",
                () -> draft.orbOffsetX,
                v -> {
                    draft.orbOffsetX = v;
                    pushOrbSettings();
                },
                CompanionOrbSettings.MIN_OFFSET, CompanionOrbSettings.MAX_OFFSET);
        y = addRightSlider(y, "Offset Y",
                () -> draft.orbOffsetY,
                v -> {
                    draft.orbOffsetY = v;
                    pushOrbSettings();
                },
                CompanionOrbSettings.MIN_OFFSET, CompanionOrbSettings.MAX_OFFSET);
        y = addRightSlider(y, "Offset Z",
                () -> draft.orbOffsetZ,
                v -> {
                    draft.orbOffsetZ = v;
                    pushOrbSettings();
                },
                CompanionOrbSettings.MIN_OFFSET, CompanionOrbSettings.MAX_OFFSET);
        return y + 4;
    }

    private void pushOrbSettings() {
        com.azscompanions.network.FabricNetworkingClient.sendOrbSettings(new FabricNetworking.OrbSettingsPayload(
                companion.getId(),
                draft.orbColorRgb,
                draft.orbBrightness,
                draft.orbFloatAmplitude,
                draft.orbFloatSpeed,
                draft.orbFloatHeight,
                draft.orbOffsetX,
                draft.orbOffsetY,
                draft.orbOffsetZ
        ));
    }

    private int addFormGroupButtons(int y, String title, CompanionForm.FormGroup group) {
        addRightLabel(y, title, 0xA0A0A0);
        y += 14;
        int col = 0;
        int half = (rightW - 6) / 2;
        for (CompanionForm form : CompanionForm.byGroup(group)) {
            boolean selected = draft.form == form;
            Button btn = Button.builder(
                    Component.literal(selected ? "[" + form.displayLabel() + "]" : form.displayLabel()),
                    b -> {
                        draft.form = form;
                        pushLiveAppearance(FabricNetworking.SettingsPayload.FLAG_FORM);
                        init();
                    }).bounds(rightX + (col % 2) * (half + 6), 0, half, 18).build();
            addRightWidget(btn, y, 18);
            col++;
            if (col % 2 == 0) {
                y += 22;
            }
        }
        if (col % 2 != 0) {
            y += 22;
        }
        return y + 6;
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
                draft.form.serializedName(),
                draft.showNameTag,
                draft.showArmor,
                flags
        ));
    }

    private static String catLabel(Category cat) {
        return switch (cat) {
            case NAME -> "Name";
            case FORM -> "Form";
            case SKIN -> "Face/Skin";
            case BODY -> "Body";
        };
    }

    private void commitAndClose() {
        syncEditBoxesToDraft();
        int flags = FabricNetworking.SettingsPayload.FLAG_NAME | FabricNetworking.SettingsPayload.FLAG_SCALE
                | FabricNetworking.SettingsPayload.FLAG_SKIN | FabricNetworking.SettingsPayload.FLAG_SLIM
                | FabricNetworking.SettingsPayload.FLAG_PROPORTIONS | FabricNetworking.SettingsPayload.FLAG_GENDER
                | FabricNetworking.SettingsPayload.FLAG_FORM | FabricNetworking.SettingsPayload.FLAG_SHOW_NAME
                | FabricNetworking.SettingsPayload.FLAG_SHOW_ARMOR;
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
                draft.form.serializedName(),
                draft.showNameTag,
                draft.showArmor,
                flags
        ));
        pushContextSkins();
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
        if (button == 0 && rightMaxScroll > 0) {
            int trackX = panelX + panelW - 10;
            if (mouseX >= trackX && mouseX <= trackX + SCROLLBAR_W
                    && mouseY >= rightViewTop && mouseY <= rightViewBottom) {
                int trackH = Math.max(1, rightViewBottom - rightViewTop);
                int thumbH = Math.max(16, (int) (trackH * (trackH / (float) (trackH + rightMaxScroll))));
                int travel = trackH - thumbH;
                int thumbY = rightViewTop + (travel <= 0 ? 0 : (int) (travel * (rightScroll / (float) rightMaxScroll)));
                if (mouseY >= thumbY && mouseY <= thumbY + thumbH) {
                    draggingScrollbar = true;
                    scrollbarDragGrab = (int) mouseY - thumbY;
                } else {
                    float rel = (float) ((mouseY - rightViewTop - thumbH / 2.0) / Math.max(1, travel));
                    rightScroll = Mth.clamp((int) (rel * rightMaxScroll), 0, rightMaxScroll);
                    applyRightScroll();
                    draggingScrollbar = true;
                    scrollbarDragGrab = thumbH / 2;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && rightMaxScroll > 0) {
            int trackH = Math.max(1, rightViewBottom - rightViewTop);
            int thumbH = Math.max(16, (int) (trackH * (trackH / (float) (trackH + rightMaxScroll))));
            int travel = trackH - thumbH;
            if (travel > 0) {
                float rel = (float) ((mouseY - rightViewTop - scrollbarDragGrab) / (float) travel);
                rightScroll = Mth.clamp((int) (rel * rightMaxScroll), 0, rightMaxScroll);
                applyRightScroll();
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (focusedContextBox() != null && focusedContextBox().keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (nameBox != null && nameBox.visible && nameBox.isFocused()
                && nameBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (focusedContextBox() != null && focusedContextBox().charTyped(codePoint, modifiers)) {
            return true;
        }
        if (nameBox != null && nameBox.visible && nameBox.isFocused()
                && nameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private EditBox focusedContextBox() {
        EditBox[] boxes = {
                sleepLocalBox, sleepUrlBox, bathLocalBox, bathUrlBox, adventureLocalBox, adventureUrlBox
        };
        for (EditBox box : boxes) {
            if (box != null && box.visible && box.isFocused()) {
                return box;
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 3, 0x55FFFFFF);

        graphics.fill(panelX + NAV_W + 2, panelY + HEADER_H, panelX + NAV_W + 3, panelY + panelH - FOOTER_H, 0x40FFFFFF);
        graphics.fill(previewX + PREVIEW_W + 2, panelY + HEADER_H, previewX + PREVIEW_W + 3, panelY + panelH - FOOTER_H, 0x40FFFFFF);

        graphics.drawString(font, "Companion Customization", panelX + 10, panelY + 8, TEXT_LABEL, false);
        graphics.drawString(font,
                topTab == TopTab.ACTIVITY ? "Activity outfits" : companion.getDisplayName().getString(),
                panelX + 10, panelY + 28, 0xA0A0A0, false);

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
