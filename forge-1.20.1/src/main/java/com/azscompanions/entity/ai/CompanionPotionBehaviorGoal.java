package com.azscompanions.entity.ai;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.util.CompanionPotionHelper;
import com.azscompanions.util.CompanionPotionHelper.Kind;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Auto-pickup nearby <em>beneficial</em> potions only (harmful/neutral ground loot is ignored).
 * Held potions (including manually given harmful splash) still help owner / throw at enemies / discard.
 */
public final class CompanionPotionBehaviorGoal extends Goal {
    private static final double PICKUP_RANGE = 7.0d;
    private static final double OWNER_APPLY_RANGE = 3.5d;
    private static final int COOLDOWN_TICKS = 50; // ~2.5s

    private final CompanionEntity companion;
    private ItemEntity nearbyPotion;
    private int actionCooldown;

    public CompanionPotionBehaviorGoal(CompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (companion.isSleeping() || companion.isSitting()) {
            return false;
        }
        if (companion.getOwner() == null) {
            return false;
        }
        if (actionCooldown > 0) {
            actionCooldown--;
            return false;
        }
        if (handPotionSlot() >= 0) {
            return true;
        }
        nearbyPotion = findNearbyPotion();
        return nearbyPotion != null && hasFreeHand();
    }

    @Override
    public boolean canContinueToUse() {
        if (actionCooldown > 0) {
            return false;
        }
        if (handPotionSlot() >= 0) {
            return true;
        }
        return nearbyPotion != null && nearbyPotion.isAlive() && hasFreeHand();
    }

    @Override
    public void stop() {
        nearbyPotion = null;
        companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        int potionSlot = handPotionSlot();
        if (potionSlot >= 0) {
            handleHeldPotion(potionSlot);
            return;
        }
        if (nearbyPotion == null || !nearbyPotion.isAlive()) {
            nearbyPotion = findNearbyPotion();
        }
        if (nearbyPotion == null || !hasFreeHand()) {
            return;
        }
        companion.getLookControl().setLookAt(nearbyPotion, 30.0f, 30.0f);
        if (companion.distanceTo(nearbyPotion) > 1.6d) {
            companion.getNavigation().moveTo(nearbyPotion, 1.15d);
            return;
        }
        pickUpPotion(nearbyPotion);
        nearbyPotion = null;
    }

    private void handleHeldPotion(int slot) {
        CompanionInventory inv = companion.getCompanionInventory();
        ItemStack stack = inv.getStackInSlot(slot);
        Kind kind = CompanionPotionHelper.classify(stack);
        Player owner = companion.getOwner();
        if (owner == null) {
            return;
        }
        companion.getLookControl().setLookAt(owner, 30.0f, 30.0f);

        if (kind == Kind.BENEFICIAL) {
            handleBeneficial(slot, stack, owner);
            return;
        }
        if (kind == Kind.HARMFUL) {
            handleHarmful(slot, stack);
            return;
        }
        // Neutral / water / awkward — discard away from owner.
        discardAway(slot, stack, owner);
    }

    private void handleBeneficial(int slot, ItemStack stack, Player owner) {
        if (CompanionPotionHelper.isThrowablePotion(stack)) {
            if (companion.distanceTo(owner) > 10.0d) {
                companion.getNavigation().moveTo(owner, 1.2d);
                return;
            }
            CompanionPotionHelper.throwPotionAt(companion, stack, owner);
            consumeOne(slot, stack);
            finishAction();
            return;
        }
        // Drinkable beneficial: apply to owner if close, else toss the bottle to them.
        if (companion.distanceTo(owner) <= OWNER_APPLY_RANGE) {
            CompanionPotionHelper.applyDrinkableTo(owner, stack);
            consumeOne(slot, stack);
            finishAction();
            return;
        }
        if (companion.distanceTo(owner) > 8.0d) {
            companion.getNavigation().moveTo(owner, 1.2d);
            return;
        }
        tossToOwner(slot, stack, owner);
        finishAction();
    }

    private void handleHarmful(int slot, ItemStack stack) {
        LivingEntity enemy = companion.getTarget();
        if (enemy != null && enemy.isAlive() && CompanionPotionHelper.isThrowablePotion(stack)) {
            if (companion.distanceTo(enemy) > 10.0d) {
                companion.getNavigation().moveTo(enemy, 1.25d);
                return;
            }
            CompanionPotionHelper.throwPotionAt(companion, stack, enemy);
            consumeOne(slot, stack);
            finishAction();
            return;
        }
        // Drinkable harmful, or splash with no enemy — discard away from owner.
        Player owner = companion.getOwner();
        discardAway(slot, stack, owner);
    }

    private void tossToOwner(int slot, ItemStack stack, Player owner) {
        ItemStack one = com.azscompanions.util.ItemStackCompat.copyWithCount(stack, 1);
        consumeOne(slot, stack);
        ItemEntity dropped = new ItemEntity(
                companion.level(), companion.getX(), companion.getY() + 0.8, companion.getZ(), one);
        Vec3 toward = owner.position().subtract(companion.position()).normalize().scale(0.35);
        dropped.setDeltaMovement(toward.x, 0.25, toward.z);
        dropped.setPickUpDelay(10);
        companion.level().addFreshEntity(dropped);
    }

    private void discardAway(int slot, ItemStack stack, Player owner) {
        ItemStack one = com.azscompanions.util.ItemStackCompat.copyWithCount(stack, 1);
        consumeOne(slot, stack);
        Vec3 away;
        if (owner != null) {
            away = companion.position().subtract(owner.position()).normalize();
        } else {
            away = companion.getLookAngle();
        }
        if (away.lengthSqr() < 0.01) {
            away = new Vec3(companion.getRandom().nextGaussian(), 0.0, companion.getRandom().nextGaussian()).normalize();
        }
        ItemEntity dropped = new ItemEntity(
                companion.level(), companion.getX(), companion.getY() + 0.6, companion.getZ(), one);
        dropped.setDeltaMovement(away.x * 0.45, 0.28, away.z * 0.45);
        dropped.setPickUpDelay(40);
        companion.level().addFreshEntity(dropped);
        finishAction();
    }

    private void consumeOne(int slot, ItemStack stack) {
        CompanionInventory inv = companion.getCompanionInventory();
        if (stack.getCount() <= 1) {
            inv.setStackInSlot(slot, ItemStack.EMPTY);
        } else {
            ItemStack copy = stack.copy();
            copy.shrink(1);
            inv.setStackInSlot(slot, copy);
        }
    }

    private void finishAction() {
        actionCooldown = COOLDOWN_TICKS;
        companion.getNavigation().stop();
    }

    private void pickUpPotion(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (!CompanionPotionHelper.isAutoPickupAllowed(stack)) {
            return;
        }
        CompanionInventory inv = companion.getCompanionInventory();
        ItemStack take = stack.copy();
        if (inv.getMainHand().isEmpty()) {
            inv.setStackInSlot(CompanionInventory.MAIN_HAND, take);
            entity.discard();
            finishAction();
            return;
        }
        if (inv.getOffHand().isEmpty()) {
            inv.setStackInSlot(CompanionInventory.OFF_HAND, take);
            entity.discard();
            finishAction();
        }
    }

    private ItemEntity findNearbyPotion() {
        AABB box = companion.getBoundingBox().inflate(PICKUP_RANGE);
        List<ItemEntity> items = companion.level().getEntitiesOfClass(
                ItemEntity.class, box, e -> CompanionPotionHelper.isAutoPickupAllowed(e.getItem()));
        ItemEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (ItemEntity item : items) {
            double dist = companion.distanceToSqr(item);
            if (dist < bestDist) {
                bestDist = dist;
                best = item;
            }
        }
        return best;
    }

    private boolean hasFreeHand() {
        CompanionInventory inv = companion.getCompanionInventory();
        return inv.getMainHand().isEmpty() || inv.getOffHand().isEmpty();
    }

    /** Prefer main hand potion, then offhand. */
    private int handPotionSlot() {
        CompanionInventory inv = companion.getCompanionInventory();
        if (CompanionPotionHelper.isPotionItem(inv.getMainHand())) {
            return CompanionInventory.MAIN_HAND;
        }
        if (CompanionPotionHelper.isPotionItem(inv.getOffHand())) {
            return CompanionInventory.OFF_HAND;
        }
        return -1;
    }
}
