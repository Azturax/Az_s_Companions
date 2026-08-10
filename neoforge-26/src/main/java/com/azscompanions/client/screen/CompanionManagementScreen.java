package com.azscompanions.client.screen;

import com.azscompanions.client.CompanionSkinTextures;
import com.azscompanions.client.PlayerSkinLookup;
import com.azscompanions.entity.CompanionBodyProportions;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.menu.CompanionManagementMenu;
import com.azscompanions.network.packet.CompanionSettingsPacket;
import com.azscompanions.network.packet.CompanionCommandPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * Companion management UI: Overview (rename/skin/size) and Body (adult proportion controls).
 */
public final class CompanionManagementScreen extends AbstractContainerScreen<CompanionManagementMenu> {
    private EditBox nameBox;
    private EditBox skinBox;
    private float editScale = CompanionEntity.DEFAULT_BODY_SCALE;
    private float editBust = CompanionBodyProportions.DEFAULT_BUST;
    private float editWaist = CompanionBodyProportions.DEFAULT_WAIST;
    private float editHips = CompanionBodyProportions.DEFAULT_HIPS;
    private float editShoulders = CompanionBodyProportions.DEFAULT_SHOULDERS;
    private float editBustOffset = CompanionBodyProportions.DEFAULT_BUST_OFFSET;
    private boolean editSlim;

    public CompanionManagementScreen(CompanionManagementMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 320, 240);
        syncFromEntity();
    }

    private void syncFromEntity() {
        CompanionEntity c = menu.companion();
        if (c == null) {
            return;
        }
        editScale = c.getBodyScale();
        editBust = c.getBust();
        editWaist = c.getWaist();
        editHips = c.getHips();
        editShoulders = c.getShoulders();
        editBustOffset = c.getBustOffset();
        editSlim = c.isSlimArms();
    }

    @Override
    protected void init() {
        super.init();
        syncFromEntity();
        int x = leftPos + 6;
        int y = topPos + 18;
        int i = 0;
        for (CompanionManagementMenu.Tab tab : CompanionManagementMenu.Tab.values()) {
            final CompanionManagementMenu.Tab t = tab;
            String label = shortLabel(tab);
            addRenderableWidget(Button.builder(Component.literal(label), b -> {
                menu.setTab(t);
                rebuildWidgets();
            }).bounds(x + (i % 5) * 62, y + (i / 5) * 20, 60, 18).build());
            i++;
        }

        CompanionEntity c = menu.companion();
        if (c == null) {
            return;
        }
        // Always available — not gated on wardrobe or first-time recruit.
        addRenderableWidget(Button.builder(Component.translatable("screen.azscompanions.customize"), b -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new CompanionCreatorScreen(c, this));
        }).bounds(leftPos + imageWidth - 100, topPos + 58, 90, 18).build());

        int cx = leftPos + 12;
        int cy = topPos + 70;

        if (menu.tab() == CompanionManagementMenu.Tab.OVERVIEW || menu.tab() == CompanionManagementMenu.Tab.VOICE) {
            nameBox = new EditBox(font, cx, cy, 160, 18, Component.literal("Name"));
            nameBox.setMaxLength(32);
            String currentName = c.getCustomName() != null ? c.getCustomName().getString() : c.getDefinition().displayName();
            nameBox.setValue(currentName);
            addRenderableWidget(nameBox);

            addRenderableWidget(Button.builder(Component.literal("Set Name"),
                    b -> applyNameAndMojangSkin())
                    .bounds(cx + 168, cy, 70, 18).build());

            addRenderableWidget(Button.builder(Component.literal("Size -"), b -> {
                editScale = Math.max(CompanionEntity.MIN_BODY_SCALE, round1(editScale - 0.1f));
                applyFlags(CompanionSettingsPacket.FLAG_SCALE);
            }).bounds(cx, cy + 22, 50, 18).build());
            addRenderableWidget(Button.builder(Component.literal("Size +"), b -> {
                editScale = Math.min(CompanionEntity.MAX_BODY_SCALE, round1(editScale + 0.1f));
                applyFlags(CompanionSettingsPacket.FLAG_SCALE);
            }).bounds(cx + 54, cy + 22, 50, 18).build());

            skinBox = new EditBox(font, cx, cy + 50, 220, 18, Component.literal("Skin"));
            skinBox.setMaxLength(256);
            skinBox.setValue(c.getSkinPath() == null ? "" : c.getSkinPath());
            skinBox.setEditable(false);
            addRenderableWidget(skinBox);

            Button slimBtn = Button.builder(Component.literal(editSlim ? "Arms: Slim" : "Arms: Wide"), b -> {
                editSlim = !editSlim;
                b.setMessage(Component.literal(editSlim ? "Arms: Slim" : "Arms: Wide"));
                applyFlags(CompanionSettingsPacket.FLAG_SLIM);
            }).bounds(cx, cy + 72, 90, 18).build();
            addRenderableWidget(slimBtn);
            addRenderableWidget(Button.builder(Component.literal("Open Inventory"), b ->
                    PacketDistributor.sendToServer(new CompanionCommandPacket(c.getId(), "OPEN_INVENTORY"))
            ).bounds(cx + 96, cy + 72, 100, 18).build());
        } else if (menu.tab() == CompanionManagementMenu.Tab.BODY) {
            addPropControls("Bust", cy, () -> editBust, v -> editBust = v,
                    CompanionBodyProportions.MIN_BUST, CompanionBodyProportions.MAX_BUST);
            addPropControls("Waist", cy + 22, () -> editWaist, v -> editWaist = v,
                    CompanionBodyProportions.MIN_WAIST, CompanionBodyProportions.MAX_WAIST);
            addPropControls("Hips", cy + 44, () -> editHips, v -> editHips = v,
                    CompanionBodyProportions.MIN_HIPS, CompanionBodyProportions.MAX_HIPS);
            addPropControls("Shoulders", cy + 66, () -> editShoulders, v -> editShoulders = v,
                    CompanionBodyProportions.MIN_SHOULDERS, CompanionBodyProportions.MAX_SHOULDERS);
            addPropControls("Bust shape", cy + 88, () -> editBustOffset, v -> editBustOffset = v,
                    CompanionBodyProportions.MIN_BUST_OFFSET, CompanionBodyProportions.MAX_BUST_OFFSET);
            addRenderableWidget(Button.builder(Component.literal("Apply Proportions"),
                    b -> applyFlags(CompanionSettingsPacket.FLAG_PROPORTIONS))
                    .bounds(cx, cy + 112, 120, 18).build());
            addRenderableWidget(Button.builder(Component.literal("Reset Defaults"), b -> {
                editBust = CompanionBodyProportions.DEFAULT_BUST;
                editWaist = CompanionBodyProportions.DEFAULT_WAIST;
                editHips = CompanionBodyProportions.DEFAULT_HIPS;
                editShoulders = CompanionBodyProportions.DEFAULT_SHOULDERS;
                editBustOffset = CompanionBodyProportions.DEFAULT_BUST_OFFSET;
                applyFlags(CompanionSettingsPacket.FLAG_PROPORTIONS);
            }).bounds(cx + 128, cy + 112, 110, 18).build());
        } else if (menu.tab() == CompanionManagementMenu.Tab.INVENTORY) {
            addRenderableWidget(Button.builder(Component.literal("Open Inventory GUI"), b ->
                    PacketDistributor.sendToServer(new CompanionCommandPacket(c.getId(), "OPEN_INVENTORY"))
            ).bounds(cx, cy, 140, 20).build());
        }
    }

    private static String shortLabel(CompanionManagementMenu.Tab tab) {
        return tab.name().charAt(0) + tab.name().substring(1).toLowerCase(Locale.ROOT);
    }

    private static float round1(float v) {
        return Math.round(v * 10f) / 10f;
    }

    private interface FloatGetter {
        float get();
    }

    private interface FloatSetter {
        void set(float v);
    }

    private void addPropControls(String ignoredLabel, int y, FloatGetter getter, FloatSetter setter, float min, float max) {
        int x = leftPos + 12;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            setter.set(Math.max(min, Math.round((getter.get() - 0.05f) * 100f) / 100f));
            applyFlags(CompanionSettingsPacket.FLAG_PROPORTIONS);
        }).bounds(x + 150, y, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            setter.set(Math.min(max, Math.round((getter.get() + 0.05f) * 100f) / 100f));
            applyFlags(CompanionSettingsPacket.FLAG_PROPORTIONS);
        }).bounds(x + 174, y, 20, 18).build());
    }

    private void applyNameAndMojangSkin() {
        CompanionEntity c = menu.companion();
        if (c == null || nameBox == null) {
            return;
        }
        String name = nameBox.getValue() == null ? "" : nameBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        if (name.equalsIgnoreCase("Kon")) {
            if (skinBox != null) {
                skinBox.setValue(CompanionSkinTextures.DEFAULT_KON.toString());
            }
            applyFlags(CompanionSettingsPacket.FLAG_NAME
                    | CompanionSettingsPacket.FLAG_SKIN
                    | CompanionSettingsPacket.FLAG_SLIM);
            return;
        }
        if (!PlayerSkinLookup.isValidUsername(name)) {
            applyFlags(CompanionSettingsPacket.FLAG_NAME);
            return;
        }
        PlayerSkinLookup.lookupPlayerAsync(name, opt -> {
            if (opt.isEmpty()) {
                applyFlags(CompanionSettingsPacket.FLAG_NAME);
                return;
            }
            var player = opt.get();
            CompanionSkinTextures.loadPlayerSkin(player.uuid(), ready -> {
                if (ready.isEmpty()) {
                    applyFlags(CompanionSettingsPacket.FLAG_NAME);
                    return;
                }
                editSlim = ready.get().slim();
                if (skinBox != null) {
                    skinBox.setValue("player:" + player.uuid());
                }
                applyFlags(CompanionSettingsPacket.FLAG_NAME
                        | CompanionSettingsPacket.FLAG_SKIN
                        | CompanionSettingsPacket.FLAG_SLIM);
            });
        });
    }

    private void applyFlags(int flags) {
        CompanionEntity c = menu.companion();
        if (c == null) {
            return;
        }
        String name = nameBox != null ? nameBox.getValue() : "";
        String skin = skinBox != null ? skinBox.getValue() : c.getSkinPath();
        PacketDistributor.sendToServer(new CompanionSettingsPacket(
                c.getId(),
                name,
                editScale,
                skin == null ? "" : skin,
                editSlim,
                c.isMale(),
                editBust,
                editWaist,
                editHips,
                editShoulders,
                editBustOffset,
                c.getForm().serializedName(),
                c.isNameTagVisible(),
                c.isArmorVisible(),
                flags
        ));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xC0101010);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 3, 0xFF8B8B8B);
        CompanionEntity c = menu.companion();
        if (c == null) {
            return;
        }
        graphics.text(font, c.getDisplayName(), leftPos + 12, topPos + 58, 0xFFFFFF, false);

        if (menu.tab() == CompanionManagementMenu.Tab.OVERVIEW || menu.tab() == CompanionManagementMenu.Tab.VOICE) {
            graphics.text(font, "Name", leftPos + 12, topPos + 60, 0xA0A0A0, false);
            graphics.text(font, String.format(Locale.ROOT, "Size: %.1f  (0.5–3.0)", editScale),
                    leftPos + 120, topPos + 96, 0xFFFFFF, false);
            graphics.text(font, "Skin (Mojang via name)", leftPos + 12, topPos + 108, 0xA0A0A0, false);
            graphics.text(font, "Mode: " + c.getMode().getSerializedName(),
                    leftPos + 12, topPos + 168, 0xA0A0A0, false);
            graphics.textWithWordWrap(font, Component.literal(
                            "Set Name looks up the Mojang profile skin."),
                    leftPos + 12, topPos + 186, 290, 0xA0A0A0);
        } else if (menu.tab() == CompanionManagementMenu.Tab.BODY) {
            int y = topPos + 74;
            drawProp(graphics, "Bust", editBust, CompanionBodyProportions.MIN_BUST, CompanionBodyProportions.MAX_BUST, y);
            drawProp(graphics, "Waist", editWaist, CompanionBodyProportions.MIN_WAIST, CompanionBodyProportions.MAX_WAIST, y + 22);
            drawProp(graphics, "Hips", editHips, CompanionBodyProportions.MIN_HIPS, CompanionBodyProportions.MAX_HIPS, y + 44);
            drawProp(graphics, "Shoulders", editShoulders, CompanionBodyProportions.MIN_SHOULDERS, CompanionBodyProportions.MAX_SHOULDERS, y + 66);
            drawProp(graphics, "Bust shape", editBustOffset, CompanionBodyProportions.MIN_BUST_OFFSET, CompanionBodyProportions.MAX_BUST_OFFSET, y + 88);
            graphics.text(font, "Adult proportions. Height/size is on Overview.",
                    leftPos + 12, topPos + 210, 0xA0A0A0, false);
        } else {
            graphics.text(font, "Tab: " + menu.tab().name(), leftPos + 12, topPos + 80, 0xFFFFFF, false);
            c.getTaskQueue().describeActive().ifPresent(desc ->
                    graphics.text(font, "Task: " + desc, leftPos + 12, topPos + 96, 0xA0A0A0, false));
        }
    }

    private void drawProp(GuiGraphicsExtractor graphics, String label, float value, float min, float max, int y) {
        graphics.text(font, String.format(Locale.ROOT, "%s: %.2f  (%.2f–%.2f)", label, value, min, max),
                leftPos + 12, y + 5, 0xFFFFFF, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (skinBox != null && skinBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
