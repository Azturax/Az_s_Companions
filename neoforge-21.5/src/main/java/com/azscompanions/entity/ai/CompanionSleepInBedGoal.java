package com.azscompanions.entity.ai;

import com.azscompanions.block.KonBedBlock;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.entity.CompanionWakeLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * At night (or when the owner is sleeping), path to the nearest usable bed and sleep.
 * Kon-named companions prefer the nearest {@link KonBedBlock}, otherwise any nearest bed.
 * Leaves bed if the owner moves farther than {@link CompanionFollowDistances#LEAVE_BED_OWNER_DISTANCE}.
 */
public final class CompanionSleepInBedGoal extends Goal {
    /** Horizontal search radius (blocks) around the companion for usable beds. */
    private static final int SEARCH_RADIUS = 48;
    private static final int SEARCH_VERTICAL = 48;
    /** Cooldown after waking from owner-distance to avoid bed thrashing. */
    private static final int WAKE_COOLDOWN_TICKS = 100;

    private final CompanionEntity companion;
    private BlockPos bedPos;
    private int recalc;
    private int wakeCooldown;

    public CompanionSleepInBedGoal(CompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (wakeCooldown > 0) {
            wakeCooldown--;
            return false;
        }
        if (companion.getMode() != CompanionMode.FOLLOW) {
            return false;
        }
        if (!(companion.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!shouldSleep(level)) {
            return false;
        }
        if (ownerTooFar()) {
            return false;
        }
        bedPos = findNearestUsableBed(level);
        return bedPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(companion.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!shouldSleep(level) || ownerTooFar()) {
            return false;
        }
        if (bedPos == null || !isUsableBed(level, bedPos)) {
            bedPos = findNearestUsableBed(level);
        }
        return bedPos != null;
    }

    @Override
    public void start() {
        recalc = 0;
    }

    @Override
    public void stop() {
        if (companion.isSleeping()) {
            companion.stopSleeping();
            CompanionWakeLoot.tryGiveOnWake(companion);
        }
        companion.getNavigation().stop();
        if (ownerTooFar()) {
            wakeCooldown = WAKE_COOLDOWN_TICKS;
        }
        bedPos = null;
    }

    @Override
    public void tick() {
        if (!(companion.level() instanceof ServerLevel level) || bedPos == null) {
            return;
        }
        if (ownerTooFar()) {
            return;
        }
        if (companion.blockPosition().closerThan(bedPos, 2.0d)) {
            if (!companion.isSleeping()) {
                if (!isUsableBed(level, bedPos)) {
                    bedPos = findNearestUsableBed(level);
                    return;
                }
                companion.getNavigation().stop();
                companion.startSleeping(bedPos);
                companion.setHomeBedPos(bedPos);
                companion.setHomePos(bedPos);
            }
            return;
        }
        if (companion.isSleeping()) {
            companion.stopSleeping();
        }
        if (--recalc <= 0) {
            recalc = 15;
            companion.getNavigation().moveTo(bedPos.getX() + 0.5d, bedPos.getY(), bedPos.getZ() + 0.5d, 1.05d);
        }
    }

    private boolean ownerTooFar() {
        Player owner = companion.getOwner();
        if (owner == null) {
            return false;
        }
        return companion.distanceTo(owner) > CompanionFollowDistances.LEAVE_BED_OWNER_DISTANCE;
    }

    private boolean shouldSleep(ServerLevel level) {
        Player owner = companion.getOwner();
        if (owner != null && owner.isSleeping()) {
            return true;
        }
        return level.isDarkOutside();
    }

    /**
     * Picks the closest usable empty bed near the companion.
     * Kon-named: nearest Kon bed first, else nearest any bed. Always nearest â€” never locks to a stored home bed.
     */
    private BlockPos findNearestUsableBed(ServerLevel level) {
        BlockPos origin = companion.blockPosition();
        boolean preferKonBed = companion.isKonNamed();
        BlockPos bestKon = null;
        int bestKonDist = Integer.MAX_VALUE;
        BlockPos bestAny = null;
        int bestAnyDist = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -SEARCH_VERTICAL; dy <= SEARCH_VERTICAL; dy++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!isUsableBed(level, cursor)) {
                        continue;
                    }
                    int dist = origin.distManhattan(cursor);
                    if (preferKonBed && isKonBed(level, cursor)) {
                        if (dist < bestKonDist) {
                            bestKonDist = dist;
                            bestKon = cursor.immutable();
                        }
                    } else if (dist < bestAnyDist) {
                        bestAnyDist = dist;
                        bestAny = cursor.immutable();
                    }
                }
            }
        }
        BlockPos nearest = bestKon != null ? bestKon : bestAny;
        if (nearest != null) {
            companion.setHomeBedPos(nearest);
            companion.setHomePos(nearest);
        }
        return nearest;
    }

    private boolean isUsableBed(ServerLevel level, BlockPos pos) {
        if (!isBedHead(level, pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BedBlock.OCCUPIED) && state.getValue(BedBlock.OCCUPIED)) {
            return false;
        }
        AABB box = new AABB(pos).inflate(0.25d);
        return level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && e.isSleeping() && e != companion).isEmpty();
    }

    private static boolean isKonBed(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof KonBedBlock;
    }

    /** True for the head half of any bed (vanilla colors, Kon bed, tagged beds). */
    public static boolean isBedHead(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.BEDS) && !(state.getBlock() instanceof BedBlock)) {
            return false;
        }
        if (!(state.getBlock() instanceof BedBlock)) {
            return false;
        }
        return !state.hasProperty(BedBlock.PART) || state.getValue(BedBlock.PART) == BedPart.HEAD;
    }
}
