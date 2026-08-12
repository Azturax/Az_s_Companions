package com.azscompanions.task;

import com.azscompanions.entity.FabricCompanionEntity;
import net.minecraft.server.level.ServerLevel;

public abstract class FabricCompanionTask {
    private final String typeId;

    protected FabricCompanionTask(String typeId) {
        this.typeId = typeId;
    }

    public String typeId() {
        return typeId;
    }

    public void start(FabricCompanionEntity companion, ServerLevel level) {
    }

    public abstract Result tick(FabricCompanionEntity companion, ServerLevel level);

    public enum Result {
        RUNNING, COMPLETED, FAILED
    }
}
