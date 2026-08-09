package com.koncompanions.item;

import com.koncompanions.entity.CompanionEntity;
import com.koncompanions.entity.CompanionRecruitment;
import com.koncompanions.entity.CompanionRegistry;
import com.koncompanions.registry.ModItems;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Loot-only Companion Charm. Unbound: first use recruits Kon and binds.
 * Bound: toggles appear (summon from stored NBT) / disappear (store + despawn).
 * Players may hold only one charm; extras are dropped.
 */
public final class CompanionCharmItem extends Item {
    public CompanionCharmItem(Properties properties) {
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
        UUID bound = CharmData.getBoundUuid(stack);

        if (bound == null) {
            CompanionEntity created = CompanionRecruitment.recruit(player, CompanionRegistry.KON_ID.toString());
            if (created != null) {
                CharmData.bind(stack, created.getUUID());
                grantKonBedOnce(player, stack);
                player.displayClientMessage(Component.translatable("message.koncompanions.charm_bound"), true);
            }
            return;
        }

        CompanionEntity living = findBoundCompanion(level, player, bound);
        if (living != null) {
            var entityTag = new net.minecraft.nbt.CompoundTag();
            living.saveWithoutId(entityTag);
            CharmData.storeCompanion(stack, entityTag, bound);
            living.discard();
            player.displayClientMessage(Component.translatable("message.koncompanions.charm_despawned"), true);
            return;
        }

        if (CharmData.hasStoredCompanion(stack)) {
            var stored = CharmData.takeStoredCompanion(stack);
            if (stored != null) {
                CompanionEntity spawned = CompanionRecruitment.spawnFromStored(player, stored, bound);
                if (spawned != null) {
                    player.displayClientMessage(Component.translatable("message.koncompanions.charm_summoned"), true);
                }
            }
            return;
        }

        // Bound but missing (lost entity / no payload): recruit replacement and rebind.
        CompanionEntity created = CompanionRecruitment.recruit(player, CompanionRegistry.KON_ID.toString());
        if (created != null) {
            CharmData.bind(stack, created.getUUID());
            player.displayClientMessage(Component.translatable("message.koncompanions.charm_bound"), true);
        }
    }

    /** First recruit only: give 1× Kon Bed and mark charm so resummons never spam beds. */
    private static void grantKonBedOnce(ServerPlayer player, ItemStack charm) {
        if (CharmData.hasGrantedBed(charm)) {
            return;
        }
        CharmData.markBedGranted(charm);
        ItemStack bed = new ItemStack(ModItems.KON_BED.get());
        if (!player.getInventory().add(bed)) {
            player.drop(bed, false);
        }
    }

    @Nullable
    private static CompanionEntity findBoundCompanion(ServerLevel level, ServerPlayer player, UUID bound) {
        Entity byId = level.getEntity(bound);
        if (byId instanceof CompanionEntity companion
                && player.getUUID().equals(companion.getOwnerUuid())) {
            return companion;
        }
        List<CompanionEntity> nearby = level.getEntitiesOfClass(
                CompanionEntity.class,
                new AABB(player.blockPosition()).inflate(256),
                c -> bound.equals(c.getUUID()) && player.getUUID().equals(c.getOwnerUuid()));
        return nearby.isEmpty() ? null : nearby.getFirst();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        enforceSingleCharm(player);
    }

    /** Drop every charm after the first found in the player inventory. */
    public static void enforceSingleCharm(Player player) {
        boolean kept = false;
        boolean dropped = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.is(ModItems.COMPANION_CHARM.get())) {
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
        if (CharmData.isBound(stack)) {
            tooltip.add(Component.translatable(
                    CharmData.hasStoredCompanion(stack)
                            ? "item.koncompanions.companion_charm.bound_stored"
                            : "item.koncompanions.companion_charm.bound_active"));
        } else {
            tooltip.add(Component.translatable("item.koncompanions.companion_charm.unbound"));
        }
    }
}
