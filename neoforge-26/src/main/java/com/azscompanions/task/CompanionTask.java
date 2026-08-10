package com.azscompanions.task;

import com.azscompanions.entity.CompanionEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Server-authoritative companion task. Implementations must never touch world state off-thread.
 */
public abstract class CompanionTask {
    private final String typeId;
    private final TaskPriority priority;
    private TaskStatus status = TaskStatus.QUEUED;
    private int progress;
    private String failureReason = "";

    protected CompanionTask(String typeId, TaskPriority priority) {
        this.typeId = typeId;
        this.priority = priority;
    }

    public String typeId() {
        return typeId;
    }

    public TaskPriority priority() {
        return priority;
    }

    public TaskStatus status() {
        return status;
    }

    public int progress() {
        return progress;
    }

    public String failureReason() {
        return failureReason;
    }

    protected void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(100, progress));
    }

    protected void setStatus(TaskStatus status) {
        this.status = status;
    }

    public final void start(CompanionEntity companion, ServerLevel level) {
        status = TaskStatus.RUNNING;
        onStart(companion, level);
    }

    public final TaskTickResult tick(CompanionEntity companion, ServerLevel level) {
        if (status == TaskStatus.PAUSED) {
            return TaskTickResult.IDLE;
        }
        if (status != TaskStatus.RUNNING) {
            return TaskTickResult.IDLE;
        }
        return onTick(companion, level);
    }

    public final void pause() {
        if (status == TaskStatus.RUNNING) {
            status = TaskStatus.PAUSED;
            onPause();
        }
    }

    public final void resume() {
        if (status == TaskStatus.PAUSED) {
            status = TaskStatus.RUNNING;
            onResume();
        }
    }

    public final TaskReport cancel(String reason) {
        status = TaskStatus.CANCELLED;
        failureReason = reason;
        onCancel(reason);
        return new TaskReport(typeId, status).message(reason).progress(progress);
    }

    public final TaskReport complete(String message) {
        status = TaskStatus.COMPLETED;
        setProgress(100);
        return new TaskReport(typeId, status).message(message).progress(100);
    }

    public final TaskReport fail(String reason) {
        status = TaskStatus.FAILED;
        failureReason = reason;
        onFail(reason);
        return new TaskReport(typeId, status).message(reason).progress(progress);
    }

    protected void onStart(CompanionEntity companion, ServerLevel level) {
    }

    protected abstract TaskTickResult onTick(CompanionEntity companion, ServerLevel level);

    protected void onPause() {
    }

    protected void onResume() {
    }

    protected void onCancel(String reason) {
    }

    protected void onFail(String reason) {
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", typeId);
        tag.putString("Status", status.name());
        tag.putInt("Progress", progress);
        tag.putString("Failure", failureReason);
        tag.putInt("Priority", priority.ordinal());
        writeExtra(tag);
        return tag;
    }

    protected void writeExtra(CompoundTag tag) {
    }

    protected void readExtra(CompoundTag tag) {
    }

    public void loadState(CompoundTag tag) {
        status = TaskStatus.valueOf(tag.getStringOr("Status", ""));
        progress = tag.getIntOr("Progress", 0);
        failureReason = tag.getStringOr("Failure", "");
        readExtra(tag);
    }

    public enum TaskTickResult {
        IDLE,
        RUNNING,
        COMPLETED,
        FAILED
    }
}
