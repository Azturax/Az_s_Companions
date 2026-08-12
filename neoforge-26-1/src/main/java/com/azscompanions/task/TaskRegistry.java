package com.azscompanions.task;

import com.azscompanions.AzsCompanions;
import com.azscompanions.task.tasks.BuildTask;
import com.azscompanions.task.tasks.ChopTreesTask;
import com.azscompanions.task.tasks.CollectItemsTask;
import com.azscompanions.task.tasks.CollectMaterialTask;
import com.azscompanions.task.tasks.CombatAssistTask;
import com.azscompanions.task.tasks.CraftTask;
import com.azscompanions.task.tasks.DepositTask;
import com.azscompanions.task.tasks.FarmTask;
import com.azscompanions.task.tasks.FollowOwnerTask;
import com.azscompanions.task.tasks.GatherTask;
import com.azscompanions.task.tasks.GuardAreaTask;
import com.azscompanions.task.tasks.MachineUseTask;
import com.azscompanions.task.tasks.MineTask;
import com.azscompanions.task.tasks.ReturnHomeTask;
import com.azscompanions.task.tasks.SleepTask;
import com.azscompanions.task.tasks.StayTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class TaskRegistry {
    private static final Map<String, Supplier<CompanionTask>> FACTORIES = new LinkedHashMap<>();

    private TaskRegistry() {
    }

    public static void register(String id, Supplier<CompanionTask> factory) {
        FACTORIES.put(id, factory);
    }

    public static void bootstrapVanillaTasks() {
        register("follow", FollowOwnerTask::new);
        register("stay", StayTask::new);
        register("guard", GuardAreaTask::new);
        register("gather", GatherTask::new);
        register("farm", FarmTask::new);
        register("chop_trees", ChopTreesTask::new);
        register("mine", MineTask::new);
        register("combat", CombatAssistTask::new);
        register("collect_items", CollectItemsTask::new);
        register("collect_material", CollectMaterialTask::new);
        register("deposit", DepositTask::new);
        register("build", BuildTask::new);
        register("craft", CraftTask::new);
        register("machine", MachineUseTask::new);
        register("sleep", SleepTask::new);
        register("return_home", ReturnHomeTask::new);
        AzsCompanions.LOGGER.info("Registered {} vanilla companion task type(s)", FACTORIES.size());
    }

    public static Optional<CompanionTask> create(String id) {
        Supplier<CompanionTask> factory = FACTORIES.get(id);
        return factory == null ? Optional.empty() : Optional.of(factory.get());
    }

    public static Map<String, Supplier<CompanionTask>> factories() {
        return Map.copyOf(FACTORIES);
    }
}
