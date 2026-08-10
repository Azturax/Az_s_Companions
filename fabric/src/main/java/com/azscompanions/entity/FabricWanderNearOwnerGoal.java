package com.azscompanions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Stroll near home bed (home-idle / Wander) or near owner when no bed. */
public final class FabricWanderNearOwnerGoal extends Goal {
    private static final int IDLE_CHANCE = 50;
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
        FabricCompanionMode mode = companion.getMode();
        if (mode == FabricCompanionMode.STAY || mode == FabricCompanionMode.SIT || companion.isSleeping()) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        if (companion.shouldActivelyFollowOwner()) {
            return false;
        }
        if (mode == FabricCompanionMode.WANDER) {
            return true;
        }
        if (mode == FabricCompanionMode.FOLLOW) {
            return companion.shouldHomeIdleNearBed()
                    || (companion.getHomeBedPos() == null && companion.isOwnerStandingAround());
        }
        return false;
    }

    private Vec3 pickWanderTarget() {
        Level level = companion.level();
        BlockPos bed = companion.getHomeBedPos();
        boolean aroundBed = companion.shouldHomeIdleNearBed()
                || (companion.getMode() == FabricCompanionMode.WANDER && bed != null && !companion.isOwnerFarFromHomeBed());
        Vec3 center;
        double minR;
        double maxR;
        if (aroundBed && bed != null) {
            center = Vec3.atBottomCenterOf(bed);
            minR = CompanionFollowDistances.HOME_IDLE_WANDER_MIN;
            maxR = CompanionFollowDistances.HOME_IDLE_WANDER_MAX;
        } else {
            Player owner = companion.getOwner();
            if (owner == null) {
                return null;
            }
            center = owner.position();
            minR = CompanionFollowDistances.IDLE_WANDER_MIN;
            maxR = CompanionFollowDistances.IDLE_WANDER_MAX;
        }
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = companion.getRandom().nextDouble() * Math.PI * 2.0d;
            double radius = minR + companion.getRandom().nextDouble() * (maxR - minR);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            BlockPos pos = BlockPos.containing(x, center.y, z);
            if (!isChunkLoaded(level, pos)) {
                continue;
            }
            BlockPos stand = findStandable(level, pos, 3);
            if (stand == null || !isChunkLoaded(level, stand)) {
                continue;
            }
            if (!level.getFluidState(stand).isEmpty() || !level.getFluidState(stand.above()).isEmpty()) {
                continue;
            }
            Vec3 candidate = Vec3.atBottomCenterOf(stand);
            Vec3 validated = DefaultRandomPos.getPosTowards(
                    companion, 12, 7, candidate, (float) (Math.PI / 2.0d));
            Vec3 chosen = validated != null ? validated : candidate;
            if (!isChunkLoaded(level, BlockPos.containing(chosen))) {
                continue;
            }
            return chosen;
        }
        return null;
    }

    private static boolean isChunkLoaded(Level level, BlockPos pos) {
        return level.hasChunkAt(pos)
                && level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
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
