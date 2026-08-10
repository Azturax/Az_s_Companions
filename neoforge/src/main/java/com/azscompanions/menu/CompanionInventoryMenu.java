package com.azscompanions.menu;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Companion inventory: backpack storage, then armor + hands + food on a dedicated equipment row.
 */
public final class CompanionInventoryMenu extends AbstractContainerMenu {
    /** Vertical space for backpack (3 rows) + equipment strip + player inv. */
    public static final int IMAGE_HEIGHT = 222;

    private final CompanionEntity companion;

    public CompanionInventoryMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (CompanionEntity) inv.player.level().getEntity(buf.readVarInt()));
    }

    public CompanionInventoryMenu(int id, Inventory playerInv, CompanionEntity companion) {
        super(ModMenus.COMPANION_INVENTORY.get(), id);
        this.companion = companion;
        CompanionInventory inv = companion.getCompanionInventory();

        // Backpack 3×9
        for (int slot = 0; slot < CompanionInventory.BACKPACK_SIZE; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            addSlot(new SlotItemHandler(inv, slot, 8 + col * 18, 18 + row * 18));
        }

        // Equipment row (bottom of companion section): armor | hands | food | cosmetics
        int eqY = 76;
        addSlot(new ArmorSlot(inv, CompanionInventory.HEAD, 8, eqY, EquipmentSlot.HEAD));
        addSlot(new ArmorSlot(inv, CompanionInventory.CHEST, 26, eqY, EquipmentSlot.CHEST));
        addSlot(new ArmorSlot(inv, CompanionInventory.LEGS, 44, eqY, EquipmentSlot.LEGS));
        addSlot(new ArmorSlot(inv, CompanionInventory.FEET, 62, eqY, EquipmentSlot.FEET));
        addSlot(new SlotItemHandler(inv, CompanionInventory.MAIN_HAND, 98, eqY));
        addSlot(new SlotItemHandler(inv, CompanionInventory.OFF_HAND, 116, eqY));
        addSlot(new SlotItemHandler(inv, CompanionInventory.FOOD, 134, eqY));
        for (int i = 0; i < CompanionInventory.COSMETIC_SLOTS; i++) {
            addSlot(new SlotItemHandler(inv, CompanionInventory.COSMETIC_START + i, 152 + i * 18, eqY));
        }

        int playerInvY = 140;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, playerInvY + 58));
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

    private static final class ArmorSlot extends SlotItemHandler {
        private final EquipmentSlot type;

        ArmorSlot(CompanionInventory inv, int index, int x, int y, EquipmentSlot type) {
            super(inv, index, x, y);
            this.type = type;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ArmorItem armor && armor.getType().getSlot() == type;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
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
