package com.azscompanions.event;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.ai.CompanionSleepInBedGoal;
import com.azscompanions.item.CompanionCharmItem;
import com.azscompanions.perk.SpecialPlayerPerks;
import com.azscompanions.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;

import java.util.Comparator;
import java.util.List;

/** World events: any bed can become home; enforce single charm on pickup; special UUID perks. */
public final class CompanionGameEvents {
    private CompanionGameEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            SpecialPlayerPerks.applyPlayerPerks(player);
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getPlacedBlock();
        if (!(state.getBlock() instanceof BedBlock)) {
            return;
        }
        BlockPos placed = event.getPos();
        BlockPos homeBed = placed;
        if (state.hasProperty(BedBlock.PART) && state.getValue(BedBlock.PART) == BedPart.FOOT) {
            BlockPos head = placed.relative(BedBlock.getConnectedDirection(state));
            if (level.getBlockState(head).getBlock() instanceof BedBlock) {
                homeBed = head;
            }
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        final BlockPos bed = homeBed;
        if (!CompanionSleepInBedGoal.isBedHead(level, bed)) {
            return;
        }
        List<CompanionEntity> owned = level.getEntitiesOfClass(
                CompanionEntity.class,
                new AABB(bed).inflate(64),
                c -> player.getUUID().equals(c.getOwnerUuid()));
        CompanionEntity nearest = owned.stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5)))
                .orElse(null);
        if (nearest != null) {
            nearest.setHomeBedPos(bed);
            nearest.setHomePos(bed);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof BedBlock)) {
            return;
        }
        BlockPos broken = event.getPos();
        BlockPos head = broken;
        if (state.hasProperty(BedBlock.PART) && state.getValue(BedBlock.PART) == BedPart.FOOT) {
            head = broken.relative(BedBlock.getConnectedDirection(state));
        }
        final BlockPos bedHead = head;
        List<CompanionEntity> nearby = level.getEntitiesOfClass(
                CompanionEntity.class,
                new AABB(bedHead).inflate(64),
                c -> bedHead.equals(c.getHomeBedPos()) || broken.equals(c.getHomeBedPos()));
        for (CompanionEntity companion : nearby) {
            companion.setHomeBedPos(null);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItem().getItem();
        if (stack.is(ModItems.COMPANION_CHARM.get())) {
            CompanionCharmItem.enforceSingleCharm(player);
        }
    }
}
