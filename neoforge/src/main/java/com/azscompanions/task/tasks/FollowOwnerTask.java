package com.azscompanions.task.tasks;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.task.CompanionTask;
import com.azscompanions.task.TaskPriority;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class FollowOwnerTask extends CompanionTask {
    public FollowOwnerTask() {
        super("follow", TaskPriority.NORMAL);
    }

    @Override
    protected void onStart(CompanionEntity companion, ServerLevel level) {
        companion.setMode(CompanionMode.FOLLOW);
    }

    @Override
    protected TaskTickResult onTick(CompanionEntity companion, ServerLevel level) {
        Player owner = companion.getOwner();
        if (owner == null) {
            return TaskTickResult.FAILED;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return TaskTickResult.RUNNING;
        }
        double dist = companion.distanceTo(owner);
        if (CompanionFollowDistances.shouldGroundTeleport(dist)
                && !CompanionFollowDistances.tooCloseToTeleport(dist)
                && companion.isOwnerExploring()) {
            companion.safeTeleportNear(owner.blockPosition());
        } else if (CompanionFollowDistances.needsFollow(dist) && companion.isOwnerExploring()) {
            Vec3 away = companion.position().subtract(owner.position());
            if (away.horizontalDistanceSqr() < 1.0e-4d) {
                away = new Vec3(1.0d, 0.0d, 0.0d);
            }
            Vec3 target = owner.position().add(
                    new Vec3(away.x, 0.0d, away.z).normalize().scale(CompanionFollowDistances.PREFERRED_DISTANCE));
            companion.getNavigation().moveTo(target.x, target.y, target.z, 1.05d);
        }
        setProgress(100);
        // Loose follow — done once inside the stop / comfort ring.
        return dist <= CompanionFollowDistances.FOLLOW_STOP ? TaskTickResult.COMPLETED : TaskTickResult.RUNNING;
    }
}
