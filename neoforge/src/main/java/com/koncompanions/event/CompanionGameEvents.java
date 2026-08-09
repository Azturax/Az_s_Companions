package com.koncompanions.event;

import com.koncompanions.block.KonBedBlock;
import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.item.CompanionCharmItem;
import com.koncompanions.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Comparator;
import java.util.List;

/** World events: Kon bed becomes home; enforce single charm on pickup. */
public final class CompanionGameEvents {
    private CompanionGameEvents() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getPlacedBlock();
        if (!(state.getBlock() instanceof KonBedBlock)) {
            return;
        }
        BlockPos placed = event.getPos();
        BlockPos homeBed = placed;
        if (state.hasProperty(BedBlock.PART) && state.getValue(BedBlock.PART) == BedPart.FOOT) {
            BlockPos head = placed.relative(BedBlock.getConnectedDirection(state));
            if (level.getBlockState(head).getBlock() instanceof KonBedBlock) {
                homeBed = head;
            }
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        final BlockPos bed = homeBed;
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
    public static void onItemPickup(ItemEntityPickupEvent.Post event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getOriginalStack();
        if (stack.is(ModItems.COMPANION_CHARM.get())) {
            CompanionCharmItem.enforceSingleCharm(player);
        }
    }
}
