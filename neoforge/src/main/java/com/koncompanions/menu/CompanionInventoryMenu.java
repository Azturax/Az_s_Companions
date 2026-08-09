package com.koncompanions.menu;

import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.inventory.CompanionInventory;
import com.koncompanions.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class CompanionInventoryMenu extends AbstractContainerMenu {
    private final CompanionEntity companion;

    public CompanionInventoryMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (CompanionEntity) inv.player.level().getEntity(buf.readVarInt()));
    }

    public CompanionInventoryMenu(int id, Inventory playerInv, CompanionEntity companion) {
        super(ModMenus.COMPANION_INVENTORY.get(), id);
        this.companion = companion;
        CompanionInventory inv = companion.getCompanionInventory();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(inv, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        addSlot(new SlotItemHandler(inv, CompanionInventory.MAIN_HAND, 8, 86));
        addSlot(new SlotItemHandler(inv, CompanionInventory.OFF_HAND, 26, 86));
        addSlot(new SlotItemHandler(inv, CompanionInventory.HEAD, 62, 86));
        addSlot(new SlotItemHandler(inv, CompanionInventory.CHEST, 80, 86));
        addSlot(new SlotItemHandler(inv, CompanionInventory.LEGS, 98, 86));
        addSlot(new SlotItemHandler(inv, CompanionInventory.FEET, 116, 86));
        addSlot(new SlotItemHandler(inv, CompanionInventory.FOOD, 152, 86));

        // Extra storage slots (formerly cosmetic clothing — clothing system removed).
        for (int i = 0; i < CompanionInventory.COSMETIC_SLOTS; i++) {
            addSlot(new SlotItemHandler(inv, CompanionInventory.COSMETIC_START + i, 62 + i * 18, 108));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 136 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 194));
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

    public static final class Provider implements MenuProvider {
        private final CompanionEntity companion;

        public Provider(CompanionEntity companion) {
            this.companion = companion;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("screen.koncompanions.inventory");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new CompanionInventoryMenu(id, inv, companion);
        }
    }
}
