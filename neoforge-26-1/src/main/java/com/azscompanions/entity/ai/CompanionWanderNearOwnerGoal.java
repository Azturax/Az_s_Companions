package com.azscompanions.entity.ai;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionFlightFollowSupport;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.perk.SpecialPlayerPerks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Happy Ghast–inspired leisurely wander for ground companions.
 * <p>
 * Slow occasional strolls, soft pauses between legs, comfortable roam radius.
 * If outside the radius → {@linkplain #tick() walk back}; never teleport.
 * While the owner is flying (special-perk flight), wanders in the air on a personal-space ring.
 */
public final class CompanionWanderNearOwnerGoal extends Goal {
    /** ~1/80 chance per tick while idle → rare starts like a lazy ghast. */
    private static final int START_CHANCE = 80;
    /** Leisurely ground speed (Happy Ghasts drift; we stroll). */
    private static final double SPEED = 0.55d;
    private static final int PAUSE_MIN = 40;
    private static final int PAUSE_MAX = 100;
    private static final int COOLDOWN_MIN = 60;
    private static final int COOLDOWN_MAX = 140;

    private final CompanionEntity companion;
    private Vec3 wanted;
    private int cooldown;
    private int pauseTicks;

    public CompanionWanderNearOwnerGoal(CompanionEntity companion) {
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
            wanted = isAirWander() ? preferredAirHold() : wanderCenter();
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
        if (wanted == null) {
            return false;
        }
        if (isAirWander()) {
            return companion.distanceToSqr(wanted.x, wanted.y, wanted.z)
                    > CompanionFlightFollowSupport.ARRIVE_EPSILON
                    * CompanionFlightFollowSupport.ARRIVE_EPSILON;
        }
        return !companion.getNavigation().isDone()
                && companion.distanceToSqr(wanted.x, wanted.y, wanted.z) > 1.25d;
    }

    @Override
    public void start() {
        pauseTicks = 0;
        if (wanted != null && !isAirWander()) {
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
        if (isAirWander()) {
            tickAirWander();
            return;
        }

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

    private void tickAirWander() {
        companion.getNavigation().stop();
        companion.setNoGravity(true);

        Player owner = companion.getOwner();
        double personal = companion.getPersonalSpace();
        if (owner != null && CompanionFollowDistances.tooClose(companion.distanceTo(owner), personal)) {
            pauseTicks = 0;
            wanted = preferredAirHold();
        }

        if (companion.getRandom().nextInt(20) == 0) {
            Vec3 look = wanted != null ? wanted : wanderCenter();
            if (look != null) {
                companion.getLookControl().setLookAt(look.x, look.y + 1.0d, look.z, 8.0f, companion.getMaxHeadXRot());
            }
        }

        if (isBeyondWanderRadius()) {
            pauseTicks = 0;
            wanted = preferredAirHold();
        }

        if (pauseTicks > 0) {
            pauseTicks--;
            Vec3 motion = companion.getDeltaMovement();
            double holdY = owner != null
                    ? owner.getY() + CompanionFlightFollowSupport.HOVER_Y
                    : companion.getY();
            double[] hold = CompanionFlightFollowSupport.holdVelocity(
                    motion.x, motion.y, motion.z, holdY, companion.getY());
            companion.setDeltaMovement(hold[0], hold[1], hold[2]);
            companion.hurtMarked = true;
            return;
        }

        if (wanted == null) {
            wanted = pickWanderTarget();
            if (wanted == null) {
                return;
            }
        }

        double distToWanted = companion.distanceToSqr(wanted.x, wanted.y, wanted.z);
        if (distToWanted <= CompanionFlightFollowSupport.ARRIVE_EPSILON
                * CompanionFlightFollowSupport.ARRIVE_EPSILON) {
            pauseTicks = PAUSE_MIN + companion.getRandom().nextInt(PAUSE_MAX - PAUSE_MIN + 1);
            wanted = null;
            return;
        }

        double[] vel = CompanionFlightFollowSupport.velocityToward(
                companion.getX(), companion.getY(), companion.getZ(),
                wanted.x, wanted.y, wanted.z,
                CompanionFlightFollowSupport.WANDER_SPEED);
        companion.setDeltaMovement(vel[0], vel[1], vel[2]);
        companion.hurtMarked = true;
    }

    private boolean isAirWander() {
        Player owner = companion.getOwner();
        return owner != null
                && SpecialPlayerPerks.isSpecial(owner)
                && SpecialPlayerPerks.isOwnerActivelyFlying(owner);
    }

    private Vec3 preferredAirHold() {
        Vec3 center = wanderCenter();
        if (center == null) {
            return null;
        }
        double preferred = CompanionFollowDistances.preferredDistance(companion.getPersonalSpace());
        double[] t = CompanionFlightFollowSupport.preferredFlightTarget(
                center.x, center.y, center.z,
                companion.getX(), companion.getZ(),
                preferred);
        return new Vec3(t[0], t[1], t[2]);
    }

    private boolean isBeyondWanderRadius() {
        Vec3 center = wanderCenter();
        if (center == null) {
            return false;
        }
        double maxR = wanderMaxRadius();
        if (isAirWander()) {
            return CompanionFlightFollowSupport.beyondAirWanderRadius(
                    companion.getX(), companion.getY(), companion.getZ(),
                    center.x, center.y, center.z,
                    maxR);
        }
        return companion.distanceToSqr(center.x, companion.getY(), center.z) > (maxR + 1.5d) * (maxR + 1.5d);
    }

    private Vec3 wanderCenter() {
        BlockPos bed = companion.getHomeBedPos();
        boolean aroundBed = companion.shouldHomeIdleNearBed()
                || (companion.getMode() == CompanionMode.WANDER && bed != null && !companion.isOwnerFarFromHomeBed());
        if (aroundBed && bed != null) {
            return Vec3.atBottomCenterOf(bed);
        }
        Player owner = companion.getOwner();
        return owner == null ? null : owner.position();
    }

    private double wanderMaxRadius() {
        BlockPos bed = companion.getHomeBedPos();
        boolean aroundBed = companion.shouldHomeIdleNearBed()
                || (companion.getMode() == CompanionMode.WANDER && bed != null && !companion.isOwnerFarFromHomeBed());
        return aroundBed
                ? CompanionFollowDistances.HOME_IDLE_WANDER_MAX
                : companion.getWanderRadius();
    }

    private boolean canWander() {
        CompanionMode mode = companion.getMode();
        if (mode == CompanionMode.STAY || mode == CompanionMode.SIT
                || companion.isSitting() || companion.isSleeping()) {
            return false;
        }
        if (companion.getTarget() != null && companion.getTarget().isAlive()) {
            return false;
        }
        if (companion.shouldActivelyFollowOwner()) {
            return false;
        }
        if (mode == CompanionMode.WANDER) {
            return true;
        }
        if (mode == CompanionMode.FOLLOW) {
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
                || (companion.getMode() == CompanionMode.WANDER && bed != null && !companion.isOwnerFarFromHomeBed());
        if (aroundBed) {
            minR = CompanionFollowDistances.HOME_IDLE_WANDER_MIN;
            maxR = CompanionFollowDistances.HOME_IDLE_WANDER_MAX;
        } else {
            minR = CompanionFollowDistances.IDLE_WANDER_MIN;
            maxR = companion.getWanderRadius();
        }

        if (isAirWander()) {
            for (int attempt = 0; attempt < 12; attempt++) {
                double angle = companion.getRandom().nextDouble() * Math.PI * 2.0d;
                double[] t = CompanionFlightFollowSupport.pickAirWanderTarget(
                        center.x, center.y, center.z,
                        companion.getPersonalSpace(),
                        minR, maxR,
                        angle,
                        companion.getRandom().nextDouble(),
                        companion.getRandom().nextDouble());
                BlockPos pos = BlockPos.containing(t[0], t[1], t[2]);
                if (!isChunkLoaded(level, pos)) {
                    continue;
                }
                if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                    continue;
                }
                return new Vec3(t[0], t[1], t[2]);
            }
            return preferredAirHold();
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
                && level.getBlockState(pos.below()).isSolidRender();
    }
}
