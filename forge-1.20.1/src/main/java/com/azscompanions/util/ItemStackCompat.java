package com.azscompanions.util;

import net.minecraft.world.item.ItemStack;

/** 1.20.1-compatible ItemStack helpers (copyWithCount arrived later). */
public final class ItemStackCompat {
    private ItemStackCompat() {
    }

    public static ItemStack copyWithCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }
}
