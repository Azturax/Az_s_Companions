package com.azscompanions.task.tasks;

import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import com.azscompanions.task.FabricCompanionTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class FabricFollowTask extends FabricCompanionTask {
    public FabricFollowTask() {
        super("follow");
    }

    @Override
    public void start(FabricCompanionEntity companion, ServerLevel level) {
        companion.setMode(FabricCompanionMode.FOLLOW);
    }

    @Override
    public Result tick(FabricCompanionEntity companion, ServerLevel level) {
        Player owner = companion.getOwner();
        if (owner == null) {
            return Result.FAILED;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return Result.RUNNING;
        }
        double dist = companion.distanceTo(owner);
        double personal = companion.getPersonalSpace();
        double followRadius = companion.getFollowRadius();
        if (CompanionFollowDistances.shouldGroundTeleport(dist, followRadius)
                && !CompanionFollowDistances.tooCloseToTeleport(dist, followRadius)
                && companion.isOwnerExploring()) {
            companion.safeTeleportNearOwner(owner);
            return Result.RUNNING;
        }
        if (CompanionFollowDistances.needsFollow(dist, personal, followRadius) && companion.isOwnerExploring()) {
            Vec3 away = companion.position().subtract(owner.position());
            if (away.horizontalDistanceSqr() < 1.0e-4d) {
                away = new Vec3(1.0d, 0.0d, 0.0d);
            }
            Vec3 target = owner.position().add(
                    new Vec3(away.x, 0.0d, away.z).normalize()
                            .scale(CompanionFollowDistances.preferredDistance(personal)));
            companion.getNavigation().moveTo(target.x, target.y, target.z, 1.05d);
            return Result.RUNNING;
        }
        return Result.COMPLETED;
    }
}
