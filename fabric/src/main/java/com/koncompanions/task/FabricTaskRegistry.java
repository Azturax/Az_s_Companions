package com.koncompanions.task;

import com.koncompanions.KonCompanionsFabric;
import com.koncompanions.task.tasks.FabricFollowTask;
import com.koncompanions.task.tasks.FabricGatherTask;
import com.koncompanions.task.tasks.FabricStayTask;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class FabricTaskRegistry {
    private static final Map<String, Supplier<FabricCompanionTask>> FACTORIES = new LinkedHashMap<>();

    private FabricTaskRegistry() {
    }

    public static void bootstrap() {
        FACTORIES.put("follow", FabricFollowTask::new);
        FACTORIES.put("stay", FabricStayTask::new);
        FACTORIES.put("gather", FabricGatherTask::new);
        KonCompanionsFabric.LOGGER.info("Registered {} Fabric companion task type(s)", FACTORIES.size());
    }

    public static Optional<FabricCompanionTask> create(String id) {
        Supplier<FabricCompanionTask> factory = FACTORIES.get(id);
        return factory == null ? Optional.empty() : Optional.of(factory.get());
    }
}
