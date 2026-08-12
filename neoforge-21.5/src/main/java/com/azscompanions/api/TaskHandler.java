package com.azscompanions.api;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.task.CompanionTask;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/** Lets external mods intercept or replace task execution. */
public interface TaskHandler {
    boolean supports(String taskType);

    Optional<CompanionTask.TaskTickResult> tick(CompanionEntity companion, ServerLevel level, CompanionTask task);
}
