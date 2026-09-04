package com.azscompanions.entity;

import com.azscompanions.block.FabricKonBedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * Night sleep in the companion's own Kon bed only.
 * If the claimed home bed is obstructed or gone, clears the claim and searches for another Kon bed.
 */
public final class FabricCompanionSleepInBedGoal extends Goal {
    private static final int WAKE_COOLDOWN_TICKS = 100;

    private final FabricCompanionEntity companion;
    private BlockPos bedPos;
    private int recalc;
    private int wakeCooldown;

    public FabricCompanionSleepInBedGoal(FabricCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (wakeCooldown > 0) {
            wakeCooldown--;
            return false;
        }
        if (companion.getMode() != FabricCompanionMode.FOLLOW) {
            return false;
        }
        if (!(companion.level() instanceof ServerLevel level) || !shouldSleep(level) || ownerTooFar()) {
            return false;
        }
        bedPos = resolveBed(level);
        return bedPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(companion.level() instanceof ServerLevel level) || !shouldSleep(level) || ownerTooFar()) {
            return false;
        }
        if (bedPos == null || !isUsableKonBed(level, bedPos)) {
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
        }
        companion.getNavigation().stop();
        if (ownerTooFar()) {
            wakeCooldown = WAKE_COOLDOWN_TICKS;
        }
        bedPos = null;
    }

    @Override
    public void tick() {
        if (!(companion.level() instanceof ServerLevel level) || bedPos == null || ownerTooFar()) {
            return;
        }
        if (companion.blockPosition().closerThan(bedPos, 2.0d)) {
            if (!companion.isSleeping()) {
                if (!isUsableKonBed(level, bedPos)) {
                    bedPos = resolveBed(level);
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
        return level.isNight();
    }

    private BlockPos resolveBed(ServerLevel level) {
        BlockPos originMc = companion.blockPosition();
        CompanionBedSleepSupport.IntPos origin =
                new CompanionBedSleepSupport.IntPos(originMc.getX(), originMc.getY(), originMc.getZ());
        BlockPos claimedMc = companion.getHomeBedPos();
        CompanionBedSleepSupport.IntPos claimed = claimedMc == null
                ? null
                : new CompanionBedSleepSupport.IntPos(claimedMc.getX(), claimedMc.getY(), claimedMc.getZ());

        if (CompanionBedSleepSupport.isClaimInvalid(claimed, pos -> isUsableKonBed(level, toBlockPos(pos)))) {
            companion.setHomeBedPos(null);
            claimed = null;
        }

        CompanionBedSleepSupport.IntPos resolved = CompanionBedSleepSupport.resolveSleepBed(
                origin, claimed, pos -> isUsableKonBed(level, toBlockPos(pos)));
        if (resolved == null) {
            return null;
        }
        BlockPos bed = toBlockPos(resolved);
        companion.setHomeBedPos(bed);
        companion.setHomePos(bed);
        return bed;
    }

    private boolean isUsableKonBed(ServerLevel level, BlockPos pos) {
        if (!isKonBedHead(level, pos)) {
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

    private static boolean isKonBedHead(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FabricKonBedBlock)) {
            return false;
        }
        return !state.hasProperty(BedBlock.PART) || state.getValue(BedBlock.PART) == BedPart.HEAD;
    }

    private static BlockPos toBlockPos(CompanionBedSleepSupport.IntPos pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    public static boolean isBedHead(ServerLevel level, BlockPos pos) {
        return isKonBedHead(level, pos);
    }
}
