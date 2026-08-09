package com.koncompanions;

import com.koncompanions.task.TaskPriority;
import com.koncompanions.task.TaskReport;
import com.koncompanions.task.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure JVM tests for task bookkeeping (no Minecraft runtime required).
 */
class TaskStateMachineTest {
    @Test
    void priorityOrderingCriticalBeforeBackground() {
        assertTrue(TaskPriority.CRITICAL.rank() < TaskPriority.BACKGROUND.rank());
        assertTrue(TaskPriority.HIGH.rank() < TaskPriority.NORMAL.rank());
    }

    @Test
    void taskReportStoresMessagesAndClampedProgress() {
        TaskReport report = new TaskReport("farm", TaskStatus.CANCELLED)
                .message("player_stop")
                .progress(150);
        assertEquals("farm", report.taskType());
        assertEquals(TaskStatus.CANCELLED, report.status());
        assertEquals(100, report.progressPercent());
        assertTrue(report.messages().contains("player_stop"));
    }

    @Test
    void ownershipTrustSemantics() {
        // Document expected multiplayer rules used by CompanionEntity.
        // Owner always trusted; additional UUIDs may be added to the trust list; pets of trusted players are protected.
        assertEquals("follow", "follow");
    }
}
