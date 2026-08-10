package com.azscompanions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Happy Ghast–inspired leisurely wander (Fabric): slow strolls, pauses, walk-back only.
 */
public final class FabricWanderNearOwnerGoal extends Goal {
    private static final int START_CHANCE = 80;
    private static final double SPEED = 0.55d;
    private static final int PAUSE_MIN = 40;
    private static final int PAUSE_MAX = 100;
    private static final int COOLDOWN_MIN = 60;
    private static final int COOLDOWN_MAX = 140;

    private final FabricCompanionEntity companion;
    private Vec3 wanted;
    private int cooldown;
    private int pauseTicks;

    public FabricWanderNearOwnerGoal(FabricCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
        if (isBeyondWanderRadius()) {
            wanted = wanderCenter();
            return wanted != null;
        }
        if (companion.getRandom().nextInt(START_CHANCE) != 0) {
            return false;
        }
        wanted = pickWanderTarget();
        return wanted != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!canWander()) {
            return false;
        }
        if (isBeyondWanderRadius()) {
            return true;
        }
        if (pauseTicks > 0) {
            return true;
        }
        return wanted != null
                && !companion.getNavigation().isDone()
                && companion.distanceToSqr(wanted.x, wanted.y, wanted.z) > 1.25d;
    }

    @Override
    public void start() {
        pauseTicks = 0;
        if (wanted != null) {
            companion.getNavigation().moveTo(wanted.x, wanted.y, wanted.z, SPEED);
        }
    }

    @Override
    public void stop() {
        wanted = null;
        pauseTicks = 0;
        companion.getNavigation().stop();
        cooldown = COOLDOWN_MIN + companion.getRandom().nextInt(COOLDOWN_MAX - COOLDOWN_MIN + 1);
    }

    @Override
    public void tick() {
        if (companion.getRandom().nextInt(20) == 0) {
            Vec3 look = wanted != null ? wanted : wanderCenter();
            if (look != null) {
                companion.getLookControl().setLookAt(look.x, look.y + 1.0d, look.z, 8.0f, companion.getMaxHeadXRot());
            }
        }

        if (isBeyondWanderRadius()) {
            pauseTicks = 0;
            Vec3 center = wanderCenter();
            if (center != null) {
                companion.getNavigation().moveTo(center.x, center.y, center.z, SPEED);
            }
            return;
        }

        if (pauseTicks > 0) {
            pauseTicks--;
            companion.getNavigation().stop();
            return;
        }

        if (wanted != null && companion.getNavigation().isDone()) {
            pauseTicks = PAUSE_MIN + companion.getRandom().nextInt(PAUSE_MAX - PAUSE_MIN + 1);
            wanted = null;
        }
    }

    private boolean isBeyondWanderRadius() {
        Vec3 center = wanderCenter();
        if (center == null) {
            return false;
        }
        double maxR = wanderMaxRadius();
        return companion.distanceToSqr(center.x, companion.getY(), center.z) > (maxR + 1.5d) * (maxR + 1.5d);
    }

    private Vec3 wanderCenter() {
        BlockPos bed = companion.getHomeBedPos();
        boolean aroundBed = companion.shouldHomeIdleNearBed()
                || (companion.getMode() == FabricCompanionMode.WANDER && bed != null && !companion.isOwnerFarFromHomeBed());
        if (aroundBed && bed != null) {
            return Vec3.atBottomCenterOf(bed);
        }
        Player owner = companion.getOwner();
        return owner == null ? null : owner.position();
    }

    private double wanderMaxRadius() {
        BlockPos bed = companion.getHomeBedPos();
        boolean aroundBed = companion.shouldHomeIdleNearBed()
                || (companion.getMode() == FabricCompanionMode.WANDER && bed != null && !companion.isOwnerFarFromHomeBed());
        return aroundBed
                ? CompanionFollowDistances.HOME_IDLE_WANDER_MAX
                : CompanionFollowDistances.IDLE_WANDER_MAX;
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
        Vec3 center = wanderCenter();
        if (center == null) {
            return null;
        }
        double minR;
        double maxR;
        BlockPos bed = companion.getHomeBedPos();
        boolean aroundBed = companion.shouldHomeIdleNearBed()
                || (companion.getMode() == FabricCompanionMode.WANDER && bed != null && !companion.isOwnerFarFromHomeBed());
        if (aroundBed) {
            minR = CompanionFollowDistances.HOME_IDLE_WANDER_MIN;
            maxR = CompanionFollowDistances.HOME_IDLE_WANDER_MAX;
        } else {
            minR = CompanionFollowDistances.IDLE_WANDER_MIN;
            maxR = CompanionFollowDistances.IDLE_WANDER_MAX;
        }

        Vec3 soft = DefaultRandomPos.getPos(companion, 8, 4);
        if (soft != null && isChunkLoaded(level, BlockPos.containing(soft))) {
            double dx = soft.x - center.x;
            double dz = soft.z - center.z;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (horiz >= minR && horiz <= maxR) {
                return soft;
            }
        }

        for (int attempt = 0; attempt < 12; attempt++) {
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
            return Vec3.atBottomCenterOf(stand);
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
