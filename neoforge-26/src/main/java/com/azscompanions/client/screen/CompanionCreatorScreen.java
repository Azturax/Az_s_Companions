package com.azscompanions.client.screen;

import com.azscompanions.client.ClientAppearanceDraft;
import com.azscompanions.client.CompanionSkinTextures;
import com.azscompanions.client.PlayerSkinLookup;
import com.azscompanions.entity.CompanionBodyProportions;
import com.azscompanions.entity.CompanionContextSkinSupport;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionForm;
import com.azscompanions.entity.CompanionFormVariants;
import com.azscompanions.entity.CompanionGender;
import com.azscompanions.network.packet.CompanionContextSkinsPacket;
import com.azscompanions.network.packet.CompanionSettingsPacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
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
public final class CompanionCreatorScreen extends Screen {
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

    private final CompanionEntity companion;
    @Nullable
    private final Screen parent;
    private final ClientAppearanceDraft draft;
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
    private final List<ScrollEntry> rightWidgets = new ArrayList<>();
    private final List<ScrollLabel> rightLabels = new ArrayList<>();
    /** Relative Y for live Mojang lookup status under the Name tab. */
    private int nameStatusContentY = -1;

    /** ~500ms debounce at 20 tps before Mojang username → skin lookup. */
    private static final int NAME_SKIN_DEBOUNCE_TICKS = 10;
    private int nameSkinDebounceTicks;
    private String pendingNameSkinLookup = "";
    private int nameSkinLookupGeneration;
    @Nullable
    private String skinStatusMessage;

    public CompanionCreatorScreen(CompanionEntity companion, @Nullable Screen parent) {
        super(Component.translatable("screen.azscompanions.creator"));
        this.companion = companion;
        this.parent = parent;
        this.draft = ClientAppearanceDraft.from(companion);
        ClientAppearanceDraft.ACTIVE = draft;
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
                        pushLiveAppearance(CompanionSettingsPacket.FLAG_SHOW_NAME);
                    }).bounds(rightX, 0, rightW, 20).build();
            addRightWidget(nameTagBtn, y, 20);
            y += 28;
            Button armorBtn = Button.builder(
                    Component.literal(draft.showArmor ? "Armor: Show" : "Armor: Hide"),
                    b -> {
                        draft.showArmor = !draft.showArmor;
                        b.setMessage(Component.literal(draft.showArmor ? "Armor: Show" : "Armor: Hide"));
                        pushLiveAppearance(CompanionSettingsPacket.FLAG_SHOW_ARMOR);
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
                pushLiveAppearance(CompanionSettingsPacket.FLAG_SLIM);
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
                        pushLiveAppearance(CompanionSettingsPacket.FLAG_GENDER | CompanionSettingsPacket.FLAG_PROPORTIONS);
                        init();
                    }).bounds(rightX, 0, half, 20).build();
            Button male = Button.builder(
                    Component.literal(draft.gender == CompanionGender.MALE ? "[Male]" : "Male"),
                    b -> {
                        draft.gender = CompanionGender.MALE;
                        pushLiveAppearance(CompanionSettingsPacket.FLAG_GENDER | CompanionSettingsPacket.FLAG_PROPORTIONS);
                        init();
                    }).bounds(rightX + half + 6, 0, half, 20).build();
            addRightWidget(female, y, 20);
            addRightWidget(male, y, 20);
            y += 26;
            y = addRightSlider(y, "Size", () -> draft.scale, v -> {
                        draft.scale = v;
                        pushLiveAppearance(CompanionSettingsPacket.FLAG_SCALE);
                    },
                    CompanionEntity.MIN_BODY_SCALE, CompanionEntity.MAX_BODY_SCALE);
            if (draft.gender.showsBust()) {
                y = addRightSlider(y, "Bust", () -> draft.bust, v -> {
                            draft.bust = v;
                            pushLiveAppearance(CompanionSettingsPacket.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_BUST, CompanionBodyProportions.MAX_BUST);
                y = addRightSlider(y, "Waist", () -> draft.waist, v -> {
                            draft.waist = v;
                            pushLiveAppearance(CompanionSettingsPacket.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_WAIST, CompanionBodyProportions.MAX_WAIST);
                y = addRightSlider(y, "Hips", () -> draft.hips, v -> {
                            draft.hips = v;
                            pushLiveAppearance(CompanionSettingsPacket.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_HIPS, CompanionBodyProportions.MAX_HIPS);
                y = addRightSlider(y, "Shoulders", () -> draft.shoulders, v -> {
                            draft.shoulders = v;
                            pushLiveAppearance(CompanionSettingsPacket.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_SHOULDERS, CompanionBodyProportions.MAX_SHOULDERS);
                y = addRightSlider(y, "Bust shape", () -> draft.bustOffset, v -> {
                            draft.bustOffset = v;
                            pushLiveAppearance(CompanionSettingsPacket.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_BUST_OFFSET, CompanionBodyProportions.MAX_BUST_OFFSET);
            } else {
                y = addRightSlider(y, "Waist", () -> draft.waist, v -> {
                            draft.waist = v;
                            pushLiveAppearance(CompanionSettingsPacket.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_WAIST, CompanionBodyProportions.MAX_WAIST);
                y = addRightSlider(y, "Hips", () -> draft.hips, v -> {
                            draft.hips = v;
                            pushLiveAppearance(CompanionSettingsPacket.FLAG_PROPORTIONS);
                        },
                        CompanionBodyProportions.MIN_HIPS, CompanionBodyProportions.MAX_HIPS);
                y = addRightSlider(y, "Shoulders", () -> draft.shoulders, v -> {
                            draft.shoulders = v;
                            pushLiveAppearance(CompanionSettingsPacket.FLAG_PROPORTIONS);
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

    private void drawScrollbar(GuiGraphicsExtractor graphics) {
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
        pushLiveAppearance(CompanionSettingsPacket.FLAG_NAME);
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
            applyDraftSkin(CompanionSkinTextures.DEFAULT_KON.toString(), draft.slimArms);
            skinStatusMessage = null;
            pushLiveAppearance(CompanionSettingsPacket.FLAG_NAME
                    | CompanionSettingsPacket.FLAG_SKIN
                    | CompanionSettingsPacket.FLAG_SLIM);
            return;
        }
        if (!PlayerSkinLookup.isValidUsername(trimmed)) {
            skinStatusMessage = "Not a valid Minecraft username";
            pushLiveAppearance(CompanionSettingsPacket.FLAG_NAME);
            return;
        }
        final int generation = ++nameSkinLookupGeneration;
        final String requested = trimmed;
        skinStatusMessage = "Looking up " + requested + "…";
        PlayerSkinLookup.lookupPlayerAsync(requested, opt -> {
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
                pushLiveAppearance(CompanionSettingsPacket.FLAG_NAME);
                return;
            }
            PlayerSkinLookup.ResolvedPlayer player = opt.get();
            UUID uuid = player.uuid();
            skinStatusMessage = "Downloading skin for " + requested + "…";
            // Update name now; keep previous skin until the texture is fully registered.
            pushLiveAppearance(CompanionSettingsPacket.FLAG_NAME);
            CompanionSkinTextures.loadPlayerSkin(uuid, ready -> {
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
                    pushLiveAppearance(CompanionSettingsPacket.FLAG_NAME);
                    return;
                }
                CompanionSkinTextures.ReadySkin skin = ready.get();
                applyDraftSkin("player:" + uuid, skin.slim());
                skinStatusMessage = "Skin: " + requested;
                pushLiveAppearance(CompanionSettingsPacket.FLAG_NAME
                        | CompanionSettingsPacket.FLAG_SKIN
                        | CompanionSettingsPacket.FLAG_SLIM);
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
        y = addContextSkinSection(y, "Sleeping", draft.sleepingSkinPath,
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
        y = addContextSkinSection(y, "Bathing", draft.bathingSkinPath,
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
        y = addContextSkinSection(y, "Adventuring", draft.adventuringSkinPath,
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
                "Local: config/azscompanions/skins/ · URL: https://…",
                0x808080);
        return y;
    }

    @FunctionalInterface
    private interface ContextApply {
        void apply(String local, String url);
    }

    private int addContextSkinSection(
            int y, String title, String current, ContextApply onApply, Runnable onClear, Consumer<EditBox[]> boxSink) {
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
        ClientPacketDistributor.sendToServer(new CompanionContextSkinsPacket(
                companion.getId(),
                draft.sleepingSkinPath == null ? "" : draft.sleepingSkinPath,
                draft.bathingSkinPath == null ? "" : draft.bathingSkinPath,
                draft.adventuringSkinPath == null ? "" : draft.adventuringSkinPath
        ));
    }

    private int addFormGroupButtons(int y, String title, CompanionForm.FormGroup group) {
        addRightLabel(y, title, 0xA0A0A0);
        y += 14;
        int col = 0;
        int half = (rightW - 6) / 2;
        int arrowW = 14;
        int gap = 1;
        for (CompanionForm form : CompanionForm.byGroup(group)) {
            boolean selected = draft.form == form;
            boolean hasVariants = CompanionFormVariants.hasVariants(form);
            int cellX = rightX + (col % 2) * (half + 6);
            String label = form.displayLabel();
            if (selected) {
                String vLabel = hasVariants
                        ? CompanionFormVariants.displayLabel(
                        CompanionFormVariants.normalize(form, draft.formVariant))
                        : "";
                label = vLabel.isEmpty()
                        ? "[" + form.displayLabel() + "]"
                        : "[" + form.displayLabel() + ": " + vLabel + "]";
            }
            if (hasVariants) {
                int midW = half - 2 * arrowW - 2 * gap;
                Button left = Button.builder(Component.literal("<"), b -> cycleFormVariant(form, -1))
                        .bounds(cellX, 0, arrowW, 18).build();
                Button mid = Button.builder(Component.literal(label), b -> selectForm(form))
                        .bounds(cellX + arrowW + gap, 0, Math.max(24, midW), 18).build();
                Button right = Button.builder(Component.literal(">"), b -> cycleFormVariant(form, 1))
                        .bounds(cellX + half - arrowW, 0, arrowW, 18).build();
                addRightWidget(left, y, 18);
                addRightWidget(mid, y, 18);
                addRightWidget(right, y, 18);
            } else {
                Button btn = Button.builder(Component.literal(label), b -> selectForm(form))
                        .bounds(cellX, 0, half, 18).build();
                addRightWidget(btn, y, 18);
            }
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

    private void selectForm(CompanionForm form) {
        draft.form = form;
        draft.formVariant = CompanionFormVariants.normalize(form, draft.formVariant);
        pushLiveAppearance(CompanionSettingsPacket.FLAG_FORM | CompanionSettingsPacket.FLAG_FORM_VARIANT);
        init();
    }

    private void cycleFormVariant(CompanionForm form, int delta) {
        draft.form = form;
        draft.formVariant = CompanionFormVariants.cycle(form, draft.formVariant, delta);
        pushLiveAppearance(CompanionSettingsPacket.FLAG_FORM | CompanionSettingsPacket.FLAG_FORM_VARIANT);
        init();
    }

    private void pushLiveAppearance(int flags) {
        ClientPacketDistributor.sendToServer(new CompanionSettingsPacket(
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
                draft.formVariant == null ? "" : draft.formVariant,
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
        int flags = CompanionSettingsPacket.FLAG_NAME | CompanionSettingsPacket.FLAG_SCALE
                | CompanionSettingsPacket.FLAG_SKIN | CompanionSettingsPacket.FLAG_SLIM
                | CompanionSettingsPacket.FLAG_PROPORTIONS | CompanionSettingsPacket.FLAG_GENDER
                | CompanionSettingsPacket.FLAG_FORM | CompanionSettingsPacket.FLAG_FORM_VARIANT
                | CompanionSettingsPacket.FLAG_SHOW_NAME
                | CompanionSettingsPacket.FLAG_SHOW_ARMOR;
        ClientPacketDistributor.sendToServer(new CompanionSettingsPacket(
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
                draft.formVariant == null ? "" : draft.formVariant,
                flags
        ));
        pushContextSkins();
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
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, PANEL_EDGE);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 3, 0x55FFFFFF);

        graphics.fill(panelX + NAV_W + 2, panelY + HEADER_H, panelX + NAV_W + 3, panelY + panelH - FOOTER_H, 0x40FFFFFF);
        graphics.fill(previewX + PREVIEW_W + 2, panelY + HEADER_H, previewX + PREVIEW_W + 3, panelY + panelH - FOOTER_H, 0x40FFFFFF);

        graphics.text(font, "Companion Customization", panelX + 10, panelY + 8, TEXT_LABEL, false);
        graphics.text(font,
                topTab == TopTab.ACTIVITY ? "Activity outfits" : companion.getDisplayName().getString(),
                panelX + 10, panelY + 28, 0xA0A0A0, false);

        graphics.fill(previewX - 2, previewY - 2, previewX + PREVIEW_W + 2, previewY + previewH + 2, 0x66000000);
        try {
            // Entity preview deferred for NeoForge 26.2 InventoryScreen API.
        } catch (Throwable ignored) {
            graphics.text(font, "Preview", previewX + PREVIEW_W / 2, previewY + previewH / 2, TEXT_LABEL);
        }

        // Hide scrollable controls from the default pass so we can clip them.
        for (ScrollEntry entry : rightWidgets) {
            entry.widget.visible = false;
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.enableScissor(rightX - 2, rightViewTop, panelX + panelW - 12, rightViewBottom);
        for (ScrollLabel label : rightLabels) {
            int screenY = rightViewTop + label.contentY - rightScroll;
            if (screenY + 24 < rightViewTop || screenY > rightViewBottom) {
                continue;
            }
            if (label.wrapped) {
                graphics.textWithWordWrap(font, label.text, rightX, screenY, rightW, label.color);
            } else {
                graphics.text(font, label.text.getString(), rightX, screenY, label.color, false);
            }
        }
        if (category == Category.NAME && nameStatusContentY >= 0
                && skinStatusMessage != null && !skinStatusMessage.isBlank()) {
            int statusY = rightViewTop + nameStatusContentY - rightScroll;
            graphics.textWithWordWrap(font, Component.literal(skinStatusMessage), rightX, statusY, rightW, 0xFFE0A0);
        }
        for (ScrollEntry entry : rightWidgets) {
            int screenY = rightViewTop + entry.contentY - rightScroll;
            boolean visible = screenY + entry.height > rightViewTop && screenY < rightViewBottom;
            entry.widget.visible = visible;
            entry.widget.active = visible;
            if (visible) {
                entry.widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
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
