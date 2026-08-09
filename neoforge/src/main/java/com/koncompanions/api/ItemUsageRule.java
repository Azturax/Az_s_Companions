package com.koncompanions.api;

import com.koncompanions.entity.CompanionEntity;
import net.minecraft.world.item.ItemStack;

public interface ItemUsageRule {
    boolean isAllowed(CompanionEntity companion, ItemStack stack);
}
