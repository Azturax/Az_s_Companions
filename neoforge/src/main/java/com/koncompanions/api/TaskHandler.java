package com.koncompanions.api;

import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.task.CompanionTask;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/** Lets external mods intercept or replace task execution. */
public interface TaskHandler {
    boolean supports(String taskType);

    Optional<CompanionTask.TaskTickResult> tick(CompanionEntity companion, ServerLevel level, CompanionTask task);
}
