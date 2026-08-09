package com.koncompanions.item;

import com.koncompanions.entity.FabricCompanionEntity;
import com.koncompanions.entity.FabricCompanionRecruitment;
import com.koncompanions.entity.FabricCompanionRegistry;
import com.koncompanions.registry.FabricModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.UUID;

public final class FabricCompanionCharmItem extends Item {
    public FabricCompanionCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            toggleOrRecruit(serverPlayer, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void toggleOrRecruit(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.serverLevel();
        UUID bound = FabricCharmData.getBoundUuid(stack);
        if (bound == null) {
            FabricCompanionEntity created = FabricCompanionRecruitment.recruitEntity(player, FabricCompanionRegistry.KON_ID.toString());
            if (created != null) {
                FabricCharmData.bind(stack, created.getUUID());
                grantKonBedOnce(player, stack);
                player.displayClientMessage(Component.translatable("message.koncompanions.charm_bound"), true);
            }
            return;
        }
        FabricCompanionEntity living = findBound(level, player, bound);
        if (living != null) {
            var tag = new net.minecraft.nbt.CompoundTag();
            living.saveWithoutId(tag);
            FabricCharmData.storeCompanion(stack, tag, bound);
            living.discard();
            player.displayClientMessage(Component.translatable("message.koncompanions.charm_despawned"), true);
            return;
        }
        if (FabricCharmData.hasStoredCompanion(stack)) {
            var stored = FabricCharmData.takeStoredCompanion(stack);
            if (stored != null) {
                FabricCompanionEntity spawned = FabricCompanionRecruitment.spawnFromStored(player, stored, bound);
                if (spawned != null) {
                    player.displayClientMessage(Component.translatable("message.koncompanions.charm_summoned"), true);
                }
            }
            return;
        }
        FabricCompanionEntity created = FabricCompanionRecruitment.recruitEntity(player, FabricCompanionRegistry.KON_ID.toString());
        if (created != null) {
            FabricCharmData.bind(stack, created.getUUID());
            player.displayClientMessage(Component.translatable("message.koncompanions.charm_bound"), true);
        }
    }

    /** First recruit only: give 1× Kon Bed and mark charm so resummons never spam beds. */
    private static void grantKonBedOnce(ServerPlayer player, ItemStack charm) {
        if (FabricCharmData.hasGrantedBed(charm)) {
            return;
        }
        FabricCharmData.markBedGranted(charm);
        ItemStack bed = new ItemStack(FabricModItems.KON_BED);
        if (!player.getInventory().add(bed)) {
            player.drop(bed, false);
        }
    }

    private static FabricCompanionEntity findBound(ServerLevel level, ServerPlayer player, UUID bound) {
        Entity byId = level.getEntity(bound);
        if (byId instanceof FabricCompanionEntity companion
                && player.getUUID().equals(companion.getOwnerUuid())) {
            return companion;
        }
        List<FabricCompanionEntity> nearby = level.getEntitiesOfClass(
                FabricCompanionEntity.class,
                new AABB(player.blockPosition()).inflate(256),
                c -> bound.equals(c.getUUID()) && player.getUUID().equals(c.getOwnerUuid()));
        return nearby.isEmpty() ? null : nearby.getFirst();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {
            enforceSingleCharm(player);
        }
    }

    public static void enforceSingleCharm(Player player) {
        boolean kept = false;
        boolean dropped = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.getItem() != FabricModItems.COMPANION_CHARM) {
                continue;
            }
            if (!kept) {
                kept = true;
                continue;
            }
            ItemStack drop = slot.copy();
            player.getInventory().setItem(i, ItemStack.EMPTY);
            ItemEntity entity = player.drop(drop, false);
            if (entity != null) {
                entity.setPickUpDelay(40);
            }
            dropped = true;
        }
        if (dropped) {
            player.displayClientMessage(Component.translatable("message.koncompanions.charm_only_one"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.koncompanions.companion_charm.desc"));
        if (FabricCharmData.isBound(stack)) {
            tooltip.add(Component.translatable(
                    FabricCharmData.hasStoredCompanion(stack)
                            ? "item.koncompanions.companion_charm.bound_stored"
                            : "item.koncompanions.companion_charm.bound_active"));
        } else {
            tooltip.add(Component.translatable("item.koncompanions.companion_charm.unbound"));
        }
    }
}
