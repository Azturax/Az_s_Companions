package com.koncompanions.api;

import com.koncompanions.entity.CompanionEntity;
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
