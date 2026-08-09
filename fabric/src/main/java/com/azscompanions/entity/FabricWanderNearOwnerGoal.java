package com.azscompanions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Casual stroll near the owner while they are standing around (idle ~2.5s).
 * Lower priority than follow — disabled while the owner is exploring.
 */
public final class FabricWanderNearOwnerGoal extends Goal {
    private static final double MIN_RADIUS = 8.0d;
    private static final double MAX_RADIUS = 16.0d;
    private static final int IDLE_CHANCE = 80;
    private static final double SPEED = 0.85d;

    private final FabricCompanionEntity companion;
    private Vec3 wanted;
    private int cooldown;

    public FabricWanderNearOwnerGoal(FabricCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!canWander()) {
            return false;
        }
        if (companion.getRandom().nextInt(IDLE_CHANCE) != 0) {
            return false;
        }
        wanted = pickWanderTarget();
        return wanted != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canWander()
                && !companion.getNavigation().isDone()
                && wanted != null
                && companion.distanceToSqr(wanted.x, wanted.y, wanted.z) > 1.0d;
    }

    @Override
    public void start() {
        if (wanted != null) {
            companion.getNavigation().moveTo(wanted.x, wanted.y, wanted.z, SPEED);
        }
    }

    @Override
    public void stop() {
        wanted = null;
        companion.getNavigation().stop();
        cooldown = 40 + companion.getRandom().nextInt(60);
    }

    private boolean canWander() {
        if (companion.getMode() != FabricCompanionMode.FOLLOW || companion.isSleeping()) {
            return false;
        }
        if (companion.getMode() == FabricCompanionMode.SIT || companion.getMode() == FabricCompanionMode.STAY) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        Player owner = companion.getOwner();
        if (owner == null || owner.isSleeping()) {
            return false;
        }
        if (!companion.isOwnerStandingAround()) {
            return false;
        }
        return companion.distanceTo(owner) <= FabricFollowOwnerGoal.FOLLOW_START_DISTANCE;
    }

    private Vec3 pickWanderTarget() {
        Player owner = companion.getOwner();
        if (owner == null) {
            return null;
        }
        Level level = companion.level();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = companion.getRandom().nextDouble() * Math.PI * 2.0d;
            double radius = MIN_RADIUS + companion.getRandom().nextDouble() * (MAX_RADIUS - MIN_RADIUS);
            double x = owner.getX() + Math.cos(angle) * radius;
            double z = owner.getZ() + Math.sin(angle) * radius;
            BlockPos pos = BlockPos.containing(x, owner.getY(), z);
            BlockPos stand = findStandable(level, pos, 3);
            if (stand == null) {
                continue;
            }
            if (!level.getFluidState(stand).isEmpty() || !level.getFluidState(stand.above()).isEmpty()) {
                continue;
            }
            Vec3 candidate = Vec3.atBottomCenterOf(stand);
            Vec3 validated = DefaultRandomPos.getPosTowards(
                    companion, 10, 7, candidate, (float) (Math.PI / 2.0d));
            return validated != null ? validated : candidate;
        }
        return null;
    }

    private static BlockPos findStandable(Level level, BlockPos around, int verticalSearch) {
        for (int dy = 0; dy <= verticalSearch; dy++) {
            BlockPos up = around.above(dy);
            BlockPos down = around.below(dy);
            if (isStandable(level, up)) {
                return up;
            }
            if (dy > 0 && isStandable(level, down)) {
                return down;
            }
        }
        return null;
    }

    private static boolean isStandable(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isSolidRender(level, pos.below());
    }
}
