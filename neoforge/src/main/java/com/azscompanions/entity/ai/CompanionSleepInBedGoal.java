package com.azscompanions.entity.ai;

import com.azscompanions.block.KonBedBlock;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionFollowDistances;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.entity.CompanionWakeLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.EnumSet;

/**
 * At night (or when the owner is sleeping), path to a bed and sleep.
 * Strongly prefers {@link KonBedBlock} / remembered home bed over vanilla beds.
 * Leaves bed if the owner moves farther than {@link CompanionFollowDistances#LEAVE_BED_OWNER_DISTANCE}.
 */
public final class CompanionSleepInBedGoal extends Goal {
    private static final int SEARCH_RADIUS = 48;
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
        bedPos = resolveBed(level);
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
        if (bedPos == null || !isBedHead(level, bedPos)) {
            bedPos = resolveBed(level);
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
        return level.isNight();
    }

    private BlockPos resolveBed(ServerLevel level) {
        BlockPos home = companion.getHomeBedPos();
        if (home != null && isBedHead(level, home)) {
            return home;
        }
        if (home != null) {
            companion.setHomeBedPos(null);
        }

        BlockPos origin = companion.blockPosition();
        BlockPos bestKon = null;
        int bestKonDist = Integer.MAX_VALUE;
        BlockPos bestAny = null;
        int bestAnyDist = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!isBedHead(level, cursor)) {
                        continue;
                    }
                    int dist = origin.distManhattan(cursor);
                    if (isKonBed(level, cursor)) {
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
