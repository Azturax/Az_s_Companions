package com.azscompanions.task;

public enum TaskPriority {
    CRITICAL(0),
    HIGH(1),
    NORMAL(2),
    LOW(3),
    BACKGROUND(4);

    private final int rank;

    TaskPriority(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
