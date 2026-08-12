package com.azscompanions.menu;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.item.CompanionCharmItem;
import com.azscompanions.registry.ModMenus;
import com.azscompanions.util.CompanionArmorRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Companion inventory layout:
 * <pre>
 * [Armor] [ 3×9 storage grid     ]
 * [col ]  [                      ]
 * [Shield][ 9-slot companion bar ]
 *
 *         [ player inventory     ]
 * </pre>
 */
public final class CompanionInventoryMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 194;
    public static final int IMAGE_HEIGHT = 220;

    public static final int ARMOR_X = 8;
    public static final int STORAGE_X = 26;
    public static final int STORAGE_Y = 18;
    /** Full-width companion hotbar under the storage grid. */
    public static final int COMPANION_HOTBAR_Y = 76;
    /** Bottom of companion panel content (armor+shield column is taller). */
    public static final int COMPANION_PANEL_BOTTOM = 112;
    /** Clear gap between companion panel and player inventory panel. */
    public static final int PANEL_GAP = 12;
    public static final int PLAYER_INV_Y = COMPANION_PANEL_BOTTOM + PANEL_GAP + 11;

    private final CompanionEntity companion;

    public CompanionInventoryMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (CompanionEntity) inv.player.level().getEntity(buf.readVarInt()));
    }

    public CompanionInventoryMenu(int id, Inventory playerInv, CompanionEntity companion) {
        super(ModMenus.COMPANION_INVENTORY.get(), id);
        this.companion = companion;
        CompanionInventory inv = companion.getCompanionInventory();

        for (int slot = 0; slot < CompanionInventory.BACKPACK_SIZE; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            addSlot(new SlotItemHandler(inv, slot, STORAGE_X + col * 18, STORAGE_Y + row * 18));
        }

        // Vertical equipment: helmet → boots → shield
        addSlot(new ArmorSlot(inv, CompanionInventory.HEAD, ARMOR_X, STORAGE_Y, EquipmentSlot.HEAD));
        addSlot(new ArmorSlot(inv, CompanionInventory.CHEST, ARMOR_X, STORAGE_Y + 18, EquipmentSlot.CHEST));
        addSlot(new ArmorSlot(inv, CompanionInventory.LEGS, ARMOR_X, STORAGE_Y + 36, EquipmentSlot.LEGS));
        addSlot(new ArmorSlot(inv, CompanionInventory.FEET, ARMOR_X, STORAGE_Y + 54, EquipmentSlot.FEET));
        addSlot(new OffhandSlot(inv, CompanionInventory.OFF_HAND, ARMOR_X, STORAGE_Y + 72));

        // 9-slot companion hotbar aligned under storage
        int hbY = COMPANION_HOTBAR_Y;
        addSlot(new SlotItemHandler(inv, CompanionInventory.MAIN_HAND, STORAGE_X, hbY));
        addSlot(new SlotItemHandler(inv, CompanionInventory.FOOD, STORAGE_X + 18, hbY));
        for (int i = 0; i < CompanionInventory.COSMETIC_SLOTS; i++) {
            addSlot(new SlotItemHandler(inv, CompanionInventory.COSMETIC_START + i, STORAGE_X + 36 + i * 18, hbY));
        }
        for (int i = 0; i < CompanionInventory.HOTBAR_EXTRA_SLOTS; i++) {
            addSlot(new SlotItemHandler(inv, CompanionInventory.HOTBAR_EXTRA_START + i, STORAGE_X + 90 + i * 18, hbY));
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

    public CompanionEntity companion() {
        return companion;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int companionSlots = CompanionInventory.TOTAL_SIZE;
            if (index < companionSlots) {
                if (!moveItemStackTo(stack, companionSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, CompanionInventory.BACKPACK_SIZE, false)) {
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
        return companion != null && companion.isAlive()
                && (companion.isOwnedBy(player) || companion.isTrusted(player));
    }

    private final class ArmorSlot extends SlotItemHandler {
        private final EquipmentSlot type;

        ArmorSlot(CompanionInventory inv, int index, int x, int y, EquipmentSlot type) {
            super(inv, index, x, y);
            this.type = type;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (companion == null || CompanionCharmItem.isCharm(stack)) {
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

    private static final class OffhandSlot extends SlotItemHandler {
        OffhandSlot(CompanionInventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !CompanionCharmItem.isCharm(stack);
        }

        @Override
        public ResourceLocation getNoItemIcon() {
            return InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
        }
    }

    public static final class Provider implements MenuProvider {
        private final CompanionEntity companion;

        public Provider(CompanionEntity companion) {
            this.companion = companion;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.azscompanions.inventory");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new CompanionInventoryMenu(id, inv, companion);
        }
    }
}
