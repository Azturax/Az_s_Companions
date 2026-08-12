package com.azscompanions.entity.inventory;

import com.azscompanions.item.CompanionCharmItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Visible companion inventory: main storage, equipment, food, and cosmetics.
 * Slot layout is stable for menus and networking.
 */
public final class CompanionInventory extends ItemStackHandler implements Container {
    public static final int BACKPACK_SIZE = 27;
    public static final int MAIN_HAND = BACKPACK_SIZE;
    public static final int OFF_HAND = BACKPACK_SIZE + 1;
    public static final int HEAD = BACKPACK_SIZE + 2;
    public static final int CHEST = BACKPACK_SIZE + 3;
    public static final int LEGS = BACKPACK_SIZE + 4;
    public static final int FEET = BACKPACK_SIZE + 5;
    public static final int FOOD = BACKPACK_SIZE + 6;
    public static final int COSMETIC_START = BACKPACK_SIZE + 7;
    public static final int COSMETIC_SLOTS = 3;
    /** Extra utility slots so the companion hotbar row is a full 9 wide. */
    public static final int HOTBAR_EXTRA_START = COSMETIC_START + COSMETIC_SLOTS;
    public static final int HOTBAR_EXTRA_SLOTS = 4;
    public static final int TOTAL_SIZE = HOTBAR_EXTRA_START + HOTBAR_EXTRA_SLOTS;

    public CompanionInventory() {
        super(TOTAL_SIZE);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return !CompanionCharmItem.isCharm(stack);
    }

    public ItemStack getMainHand() {
        return getStackInSlot(MAIN_HAND);
    }

    public ItemStack getOffHand() {
        return getStackInSlot(OFF_HAND);
    }

    public ItemStack getFoodSlot() {
        return getStackInSlot(FOOD);
    }

    public ItemStack getCosmetic(int index) {
        if (index < 0 || index >= COSMETIC_SLOTS) {
            return ItemStack.EMPTY;
        }
        return getStackInSlot(COSMETIC_START + index);
    }

    public boolean hasCosmeticClothing() {
        for (int i = 0; i < COSMETIC_SLOTS; i++) {
            if (!getStackInSlot(COSMETIC_START + i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean isFull() {
        for (int i = 0; i < BACKPACK_SIZE; i++) {
            if (getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public ItemStack insertItemAuto(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < BACKPACK_SIZE && !remaining.isEmpty(); i++) {
            remaining = insertItem(i, remaining, simulate);
        }
        return remaining;
    }

    @Override
    public int getContainerSize() {
        return getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getSlots(); i++) {
            if (!getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = getStackInSlot(slot);
        setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setStackInSlot(slot, stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getSlots(); i++) {
            setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < getSlots(); i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", i);
                list.add(stack.save(slotTag));
            }
        }
        tag.put("Items", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        clearContent();
        ListTag list = tag.getList("Items", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slotTag = list.getCompound(i);
            int slot = slotTag.getInt("Slot");
            if (slot >= 0 && slot < getSlots()) {
                setStackInSlot(slot, ItemStack.of(slotTag));
            }
        }
    }

    /** Convenience for unit tests without a full world. */
    public void copyFrom(ContainerHelper ignored) {
    }
}
