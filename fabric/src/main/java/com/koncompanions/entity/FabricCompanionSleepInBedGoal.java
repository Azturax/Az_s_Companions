package com.koncompanions.entity;

import com.koncompanions.block.FabricKonBedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.EnumSet;

public final class FabricCompanionSleepInBedGoal extends Goal {
    private static final int SEARCH_RADIUS = 48;

    private final FabricCompanionEntity companion;
    private BlockPos bedPos;
    private int recalc;

    public FabricCompanionSleepInBedGoal(FabricCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (companion.getMode() != FabricCompanionMode.FOLLOW) {
            return false;
        }
        if (!(companion.level() instanceof ServerLevel level) || !shouldSleep(level)) {
            return false;
        }
        bedPos = resolveBed(level);
        return bedPos != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(companion.level() instanceof ServerLevel level) || !shouldSleep(level)) {
            return false;
        }
        if (bedPos == null || !isKonBedHead(level, bedPos)) {
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
        bedPos = null;
    }

    @Override
    public void tick() {
        if (!(companion.level() instanceof ServerLevel) || bedPos == null) {
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

    private boolean shouldSleep(ServerLevel level) {
        Player owner = companion.getOwner();
        if (owner != null && owner.isSleeping()) {
            return true;
        }
        return level.isNight();
    }

    private BlockPos resolveBed(ServerLevel level) {
        BlockPos home = companion.getHomeBedPos();
        if (home != null && isKonBedHead(level, home)) {
            return home;
        }
        if (home != null) {
            companion.setHomeBedPos(null);
        }
        BlockPos origin = companion.blockPosition();
        BlockPos nearest = null;
        int best = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!isKonBedHead(level, cursor)) {
                        continue;
                    }
                    int dist = origin.distManhattan(cursor);
                    if (dist < best) {
                        best = dist;
                        nearest = cursor.immutable();
                    }
                }
            }
        }
        if (nearest != null) {
            companion.setHomeBedPos(nearest);
            companion.setHomePos(nearest);
        }
        return nearest;
    }

    private static boolean isKonBedHead(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FabricKonBedBlock)) {
            return false;
        }
        return !state.hasProperty(BedBlock.PART) || state.getValue(BedBlock.PART) == BedPart.HEAD;
    }
}
