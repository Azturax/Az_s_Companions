package com.azscompanions.api;

import com.azscompanions.entity.CompanionEntity;
import net.minecraft.world.item.ItemStack;

public interface ItemUsageRule {
    boolean isAllowed(CompanionEntity companion, ItemStack stack);
}
