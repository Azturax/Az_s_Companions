package com.azscompanions.menu;

import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.inventory.FabricCompanionInventory;
import com.azscompanions.item.FabricCompanionCharmItem;
import com.azscompanions.registry.FabricModScreenHandlers;
import com.azscompanions.util.CompanionArmorRules;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Companion inventory: armor+shield column, 3×9 storage, 9-slot hotbar, gapped player inv.
 */
public final class FabricCompanionInventoryMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 194;
    public static final int IMAGE_HEIGHT = 220;

    public static final int ARMOR_X = 8;
    public static final int STORAGE_X = 26;
    public static final int STORAGE_Y = 18;
    public static final int COMPANION_HOTBAR_Y = 76;
    public static final int COMPANION_PANEL_BOTTOM = 112;
    public static final int PANEL_GAP = 12;
    public static final int PLAYER_INV_Y = COMPANION_PANEL_BOTTOM + PANEL_GAP + 11;

    private final FabricCompanionEntity companion;

    public FabricCompanionInventoryMenu(int syncId, Inventory playerInv, int entityId) {
        super(FabricModScreenHandlers.INVENTORY, syncId);
        var entity = playerInv.player.level().getEntity(entityId);
        this.companion = entity instanceof FabricCompanionEntity c ? c : null;
        if (companion == null) {
            return;
        }
        FabricCompanionInventory inv = companion.getCompanionInventory();

        for (int slot = 0; slot < FabricCompanionInventory.BACKPACK_SIZE; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            addSlot(new Slot(inv, slot, STORAGE_X + col * 18, STORAGE_Y + row * 18));
        }

        addSlot(new ArmorSlot(inv, FabricCompanionInventory.HEAD, ARMOR_X, STORAGE_Y, EquipmentSlot.HEAD));
        addSlot(new ArmorSlot(inv, FabricCompanionInventory.CHEST, ARMOR_X, STORAGE_Y + 18, EquipmentSlot.CHEST));
        addSlot(new ArmorSlot(inv, FabricCompanionInventory.LEGS, ARMOR_X, STORAGE_Y + 36, EquipmentSlot.LEGS));
        addSlot(new ArmorSlot(inv, FabricCompanionInventory.FEET, ARMOR_X, STORAGE_Y + 54, EquipmentSlot.FEET));
        addSlot(new OffhandSlot(inv, FabricCompanionInventory.OFF_HAND, ARMOR_X, STORAGE_Y + 72));

        int hbY = COMPANION_HOTBAR_Y;
        addSlot(new Slot(inv, FabricCompanionInventory.MAIN_HAND, STORAGE_X, hbY));
        addSlot(new Slot(inv, FabricCompanionInventory.FOOD, STORAGE_X + 18, hbY));
        for (int i = 0; i < FabricCompanionInventory.COSMETIC_SLOTS; i++) {
            addSlot(new Slot(inv, FabricCompanionInventory.COSMETIC_START + i, STORAGE_X + 36 + i * 18, hbY));
        }
        for (int i = 0; i < FabricCompanionInventory.HOTBAR_EXTRA_SLOTS; i++) {
            addSlot(new Slot(inv, FabricCompanionInventory.HOTBAR_EXTRA_START + i, STORAGE_X + 90 + i * 18, hbY));
        }

        int playerInvY = PLAYER_INV_Y;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, STORAGE_X + col * 18, playerInvY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, STORAGE_X + col * 18, playerInvY + 58));
        }
    }

    public FabricCompanionEntity companion() {
        return companion;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int companionSlots = FabricCompanionInventory.TOTAL_SIZE;
            if (index < companionSlots) {
                if (!moveItemStackTo(stack, companionSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, FabricCompanionInventory.BACKPACK_SIZE, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return companion != null && companion.isAlive() && companion.isOwnedBy(player)
                && companion.distanceTo(player) < 8.0d;
    }

    private final class ArmorSlot extends Slot {
        private final EquipmentSlot type;

        ArmorSlot(FabricCompanionInventory inv, int index, int x, int y, EquipmentSlot type) {
            super(inv, index, x, y);
            this.type = type;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (companion == null || FabricCompanionCharmItem.isCharm(stack)) {
                return false;
            }
            return CompanionArmorRules.mayPlaceInArmorSlot(companion.getForm(), type, stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public ResourceLocation getNoItemIcon() {
            return switch (type) {
                case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
                case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
                case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
                case FEET -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
                default -> null;
            };
        }
    }

    private static final class OffhandSlot extends Slot {
        OffhandSlot(FabricCompanionInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !FabricCompanionCharmItem.isCharm(stack);
        }

        @Override
        public ResourceLocation getNoItemIcon() {
            return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
        }
    }

    public static final class ExtendedProvider implements ExtendedScreenHandlerFactory<Integer> {
        private final FabricCompanionEntity companion;

        public ExtendedProvider(FabricCompanionEntity companion) {
            this.companion = companion;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.azscompanions.inventory");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new FabricCompanionInventoryMenu(id, inv, companion.getId());
        }

        @Override
        public Integer getScreenOpeningData(ServerPlayer player) {
            return companion.getId();
        }
    }
}
