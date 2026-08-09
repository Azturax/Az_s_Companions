package com.koncompanions.voice;

import com.koncompanions.entity.CompanionDefinition;

import java.util.List;

public enum DialogueCategory {
    GREETING,
    IDLE,
    TASK_PROGRESS,
    DANGER,
    SUCCESS,
    LOW_HEALTH,
    HUNGER,
    INVENTORY_FULL,
    RETURN_HOME;

    public List<String> lines(CompanionDefinition.DialogueSet set) {
        return switch (this) {
            case GREETING -> set.greetings();
            case IDLE -> set.idle();
            case TASK_PROGRESS -> set.taskProgress();
            case DANGER -> set.danger();
            case SUCCESS -> set.success();
            case LOW_HEALTH -> set.lowHealth();
            case HUNGER -> set.hunger();
            case INVENTORY_FULL -> set.inventoryFull();
            case RETURN_HOME -> set.returnHome();
        };
    }
}
