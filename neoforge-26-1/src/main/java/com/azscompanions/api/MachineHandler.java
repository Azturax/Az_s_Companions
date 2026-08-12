package com.azscompanions.api;

import com.azscompanions.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface MachineHandler {
    enum Result {
        DONE,
        RUNNING,
        FAILED,
        SKIP
    }

    boolean canHandle(ServerLevel level, BlockPos pos, CompanionEntity companion);

    Result interact(ServerLevel level, BlockPos pos, CompanionEntity companion);
}
