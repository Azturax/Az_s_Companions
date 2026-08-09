package com.azscompanions.entity.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class FabricCompanionInventory extends SimpleContainer {
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
    public static final int TOTAL_SIZE = COSMETIC_START + COSMETIC_SLOTS;

    public FabricCompanionInventory() {
        super(TOTAL_SIZE);
    }

    public ItemStack getMainHand() {
        return getItem(MAIN_HAND);
    }

    public ItemStack getOffHand() {
        return getItem(OFF_HAND);
    }

    public ItemStack getCosmetic(int index) {
        if (index < 0 || index >= COSMETIC_SLOTS) {
            return ItemStack.EMPTY;
        }
        return getItem(COSMETIC_START + index);
    }

    public boolean hasCosmeticClothing() {
        for (int i = 0; i < COSMETIC_SLOTS; i++) {
            if (!getItem(COSMETIC_START + i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean isFull() {
        for (int i = 0; i < BACKPACK_SIZE; i++) {
            if (getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public ItemStack insertItemAuto(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < BACKPACK_SIZE && !remaining.isEmpty(); i++) {
            remaining = addItem(remaining);
        }
        return remaining;
    }

    public ListTag createTag(HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", i);
                list.add(stack.save(provider, slotTag));
            }
        }
        return list;
    }

    public void fromTag(ListTag list, HolderLookup.Provider provider) {
        clearContent();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slotTag = list.getCompound(i);
            int slot = slotTag.getInt("Slot");
            if (slot >= 0 && slot < getContainerSize()) {
                setItem(slot, ItemStack.parse(provider, slotTag).orElse(ItemStack.EMPTY));
            }
        }
    }
}
