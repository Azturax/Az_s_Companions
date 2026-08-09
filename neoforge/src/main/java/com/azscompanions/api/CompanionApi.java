package com.azscompanions.api;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionDefinition;
import com.azscompanions.entity.CompanionRegistry;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Public extension API for other mods.
 * Register during common setup before {@link #lockBootstrap()}.
 */
public final class CompanionApi {
    private static final List<TaskHandler> TASK_HANDLERS = new ArrayList<>();
    private static final List<WorkstationHandler> WORKSTATION_HANDLERS = new ArrayList<>();
    private static final List<MachineHandler> MACHINE_HANDLERS = new ArrayList<>();
    private static final List<ItemUsageRule> ITEM_USAGE_RULES = new ArrayList<>();
    private static boolean locked;

    private CompanionApi() {
    }

    public static void registerCompanion(CompanionDefinition definition) {
        ensureOpen();
        CompanionRegistry.register(definition);
    }

    public static void registerTask(String id, Supplier<CompanionTask> factory) {
        ensureOpen();
        TaskRegistry.register(id, factory);
    }

    public static void registerTaskHandler(TaskHandler handler) {
        ensureOpen();
        TASK_HANDLERS.add(handler);
    }

    public static void registerWorkstationHandler(WorkstationHandler handler) {
        ensureOpen();
        WORKSTATION_HANDLERS.add(handler);
    }

    public static void registerMachineHandler(MachineHandler handler) {
        ensureOpen();
        MACHINE_HANDLERS.add(handler);
    }

    public static void registerItemUsageRule(ItemUsageRule rule) {
        ensureOpen();
        ITEM_USAGE_RULES.add(rule);
    }

    public static List<TaskHandler> taskHandlers() {
        return List.copyOf(TASK_HANDLERS);
    }

    public static List<WorkstationHandler> workstationHandlers() {
        return List.copyOf(WORKSTATION_HANDLERS);
    }

    public static List<MachineHandler> machineHandlers() {
        return List.copyOf(MACHINE_HANDLERS);
    }

    public static List<ItemUsageRule> itemUsageRules() {
        return List.copyOf(ITEM_USAGE_RULES);
    }

    public static void lockBootstrap() {
        locked = true;
        AzsCompanions.LOGGER.debug("CompanionApi bootstrap locked");
    }

    private static void ensureOpen() {
        if (locked) {
            throw new IllegalStateException("CompanionApi is locked; register during common setup");
        }
    }
}
