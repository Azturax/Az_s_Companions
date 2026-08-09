package com.koncompanions.api;

import com.koncompanions.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;

public interface WorkstationHandler {
    boolean canHandle(ServerLevel level, BlockPos pos);

    boolean craft(ServerLevel level, BlockPos pos, CompanionEntity companion, Recipe<?> recipe);
}
