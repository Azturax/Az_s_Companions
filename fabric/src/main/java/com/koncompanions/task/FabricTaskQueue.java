package com.koncompanions.task;

import com.koncompanions.entity.FabricCompanionEntity;
import com.koncompanions.entity.FabricCompanionMode;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.Deque;

public final class FabricTaskQueue {
    private final FabricCompanionEntity companion;
    private final Deque<FabricCompanionTask> queue = new ArrayDeque<>();
    private FabricCompanionTask active;

    public FabricTaskQueue(FabricCompanionEntity companion) {
        this.companion = companion;
    }

    public void enqueue(FabricCompanionTask task) {
        queue.add(task);
    }

    public void tick(ServerLevel level) {
        if (active == null) {
            active = queue.pollFirst();
            if (active != null) {
                active.start(companion, level);
                companion.setMode(FabricCompanionMode.TASK);
            }
        }
        if (active == null) {
            return;
        }
        FabricCompanionTask.Result result = active.tick(companion, level);
        if (result == FabricCompanionTask.Result.COMPLETED || result == FabricCompanionTask.Result.FAILED) {
            active = null;
            if (queue.isEmpty()) {
                companion.setMode(FabricCompanionMode.FOLLOW);
            }
        }
    }

    public void cancelActive() {
        active = null;
        companion.setMode(FabricCompanionMode.FOLLOW);
    }

    public FabricCompanionTask getActive() {
        return active;
    }
}
