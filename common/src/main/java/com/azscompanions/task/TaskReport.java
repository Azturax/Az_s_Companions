package com.azscompanions.task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TaskReport {
    private final String taskType;
    private final TaskStatus status;
    private final Instant finishedAt;
    private final List<String> messages = new ArrayList<>();
    private int progressPercent;

    public TaskReport(String taskType, TaskStatus status) {
        this.taskType = taskType;
        this.status = status;
        this.finishedAt = Instant.now();
    }

    public TaskReport message(String message) {
        messages.add(message);
        return this;
    }

    public TaskReport progress(int percent) {
        this.progressPercent = Math.max(0, Math.min(100, percent));
        return this;
    }

    public String taskType() {
        return taskType;
    }

    public TaskStatus status() {
        return status;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public List<String> messages() {
        return List.copyOf(messages);
    }

    public int progressPercent() {
        return progressPercent;
    }
}
