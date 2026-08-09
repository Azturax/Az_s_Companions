package com.azscompanions.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Casual stroll near the owner (idle free wander 24–40, or comfort-ring stroll while exploring).
 */
public final class FabricWanderNearOwnerGoal extends Goal {
    private static final int IDLE_CHANCE = 60;
    private static final int EXPLORE_CHANCE = 100;
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
        boolean freeWander = companion.getMode() == FabricCompanionMode.WANDER;
        int chance = freeWander || companion.isOwnerStandingAround() ? IDLE_CHANCE : EXPLORE_CHANCE;
        if (companion.getRandom().nextInt(chance) != 0) {
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
        boolean freeWander = mode == FabricCompanionMode.WANDER;
        if ((!freeWander && mode != FabricCompanionMode.FOLLOW) || companion.isSleeping()) {
            return false;
        }
        if (mode == FabricCompanionMode.SIT || mode == FabricCompanionMode.STAY) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        Player owner = companion.getOwner();
        if (owner == null || owner.isSleeping()) {
            return false;
        }
        double dist = companion.distanceTo(owner);
        if (CompanionFollowDistances.tooClose(dist)) {
            return false;
        }
        if (freeWander || companion.isOwnerStandingAround()) {
            return dist <= CompanionFollowDistances.IDLE_WANDER_MAX + 4.0d;
        }
        return dist <= CompanionFollowDistances.FOLLOW_START;
    }

    private Vec3 pickWanderTarget() {
        Player owner = companion.getOwner();
        if (owner == null) {
            return null;
        }
        boolean freeRing = companion.getMode() == FabricCompanionMode.WANDER || companion.isOwnerStandingAround();
        double minR = freeRing
                ? CompanionFollowDistances.IDLE_WANDER_MIN
                : CompanionFollowDistances.MIN_PERSONAL_SPACE + 0.5d;
        double maxR = freeRing
                ? CompanionFollowDistances.IDLE_WANDER_MAX
                : CompanionFollowDistances.COMFORT_MAX;
        Level level = companion.level();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = companion.getRandom().nextDouble() * Math.PI * 2.0d;
            double radius = minR + companion.getRandom().nextDouble() * (maxR - minR);
            double x = owner.getX() + Math.cos(angle) * radius;
            double z = owner.getZ() + Math.sin(angle) * radius;
            BlockPos pos = BlockPos.containing(x, owner.getY(), z);
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
            if (owner.distanceToSqr(stand.getX() + 0.5d, stand.getY(), stand.getZ() + 0.5d)
                    < CompanionFollowDistances.MIN_PERSONAL_SPACE * CompanionFollowDistances.MIN_PERSONAL_SPACE) {
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
