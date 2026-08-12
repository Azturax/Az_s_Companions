package com.azscompanions.task;

import com.azscompanions.AzsCompanions;
import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.voice.DialogueCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Priority queue with pause/resume, cancellation, and task reports.
 * All ticking happens on the server thread.
 */
public final class TaskQueue {
    private final CompanionEntity companion;
    private final Deque<CompanionTask> queue = new ArrayDeque<>();
    private final List<TaskReport> recentReports = new ArrayList<>();
    @Nullable
    private CompanionTask active;

    public TaskQueue(CompanionEntity companion) {
        this.companion = companion;
    }

    public void enqueue(CompanionTask task) {
        queue.add(task);
        sortQueue();
        if (ServerConfig.LOG_TASK_EVENTS.get()) {
            AzsCompanions.LOGGER.debug("Companion {} queued task {}", companion.getUUID(), task.typeId());
        }
    }

    public void tick(ServerLevel level) {
        if (active == null) {
            promoteNext();
        }
        if (active == null) {
            return;
        }
        companion.setMode(CompanionMode.TASK);
        CompanionTask.TaskTickResult result = active.tick(companion, level);
        switch (result) {
            case COMPLETED -> finishActive(active.complete("completed"));
            case FAILED -> finishActive(active.fail(active.failureReason().isEmpty() ? "failed" : active.failureReason()));
            case RUNNING, IDLE -> {
            }
        }
    }

    private void promoteNext() {
        sortQueue();
        active = queue.pollFirst();
        if (active != null) {
            active.start(companion, (ServerLevel) companion.level());
            companion.speak(DialogueCategory.TASK_PROGRESS);
        }
    }

    private void finishActive(TaskReport report) {
        recentReports.add(0, report);
        if (recentReports.size() > 20) {
            recentReports.remove(recentReports.size() - 1);
        }
        if (report.status() == TaskStatus.COMPLETED) {
            companion.speak(DialogueCategory.SUCCESS);
        }
        active = null;
        if (queue.isEmpty()) {
            companion.setMode(CompanionMode.FOLLOW);
        }
    }

    public void pauseActive() {
        if (active != null) {
            active.pause();
        }
    }

    public void resumeActive() {
        if (active != null) {
            active.resume();
        }
    }

    public TaskReport cancelActive(String reason) {
        if (active == null) {
            return new TaskReport("none", TaskStatus.CANCELLED).message("no_active_task");
        }
        TaskReport report = active.cancel(reason);
        finishActive(report);
        return report;
    }

    public void clear() {
        cancelActive("cleared");
        queue.clear();
    }

    @Nullable
    public CompanionTask getActive() {
        return active;
    }

    public List<CompanionTask> queued() {
        return List.copyOf(queue);
    }

    public List<TaskReport> recentReports() {
        return List.copyOf(recentReports);
    }

    public Optional<String> describeActive() {
        if (active == null) {
            return Optional.empty();
        }
        return Optional.of(active.typeId() + " (" + active.progress() + "%) [" + active.status() + "]");
    }

    private void sortQueue() {
        List<CompanionTask> sorted = new ArrayList<>(queue);
        sorted.sort(Comparator.comparingInt(t -> t.priority().rank()));
        queue.clear();
        queue.addAll(sorted);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        if (active != null) {
            list.add(active.save());
        }
        for (CompanionTask task : queue) {
            list.add(task.save());
        }
        tag.put("Entries", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        queue.clear();
        active = null;
        ListTag list = tag.getListOrEmpty("Entries");
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            TaskRegistry.create(entry.getStringOr("Type", "")).ifPresent(task -> {
                task.loadState(entry);
                if (task.status() == TaskStatus.RUNNING || task.status() == TaskStatus.PAUSED) {
                    active = task;
                } else {
                    queue.add(task);
                }
            });
        }
    }
}
