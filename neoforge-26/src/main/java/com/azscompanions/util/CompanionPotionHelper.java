package com.azscompanions.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Classifies potion items and helps companions throw/use them.
 */
public final class CompanionPotionHelper {
    public enum Kind {
        NONE,
        BENEFICIAL,
        HARMFUL,
        NEUTRAL
    }

    private CompanionPotionHelper() {
    }

    public static boolean isPotionItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    public static boolean isThrowablePotion(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION));
    }

    public static boolean isDrinkablePotion(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.POTION);
    }

    /** Ground-loot filter: only auto-pickup purely beneficial potions (skip harmful + neutral). */
    public static boolean isAutoPickupAllowed(ItemStack stack) {
        return classify(stack) == Kind.BENEFICIAL;
    }

    public static Kind classify(ItemStack stack) {
        if (!isPotionItem(stack)) {
            return Kind.NONE;
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        boolean beneficial = false;
        boolean harmful = false;
        boolean any = false;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            any = true;
            MobEffectCategory category = effect.getEffect().value().getCategory();
            if (category == MobEffectCategory.BENEFICIAL) {
                beneficial = true;
            } else if (category == MobEffectCategory.HARMFUL) {
                harmful = true;
            }
        }
        if (!any) {
            return Kind.NEUTRAL;
        }
        if (harmful && !beneficial) {
            return Kind.HARMFUL;
        }
        if (beneficial && !harmful) {
            return Kind.BENEFICIAL;
        }
        if (harmful) {
            return Kind.HARMFUL;
        }
        if (beneficial) {
            return Kind.BENEFICIAL;
        }
        return Kind.NEUTRAL;
    }

    /** Throw splash/lingering potion projectile from thrower toward target position. */
    public static void throwPotionAt(LivingEntity thrower, ItemStack potionStack, LivingEntity target) {
        Level level = thrower.level();
        if (level.isClientSide() || target == null || potionStack.isEmpty()) {
            return;
        }
        ThrownSplashPotion thrown = new ThrownSplashPotion(level, thrower, potionStack.copyWithCount(1));
        Vec3 from = thrower.getEyePosition();
        Vec3 to = target.getEyePosition().add(0.0, target.getBbHeight() * 0.15, 0.0);
        Vec3 delta = to.subtract(from);
        thrown.shoot(delta.x, delta.y + 0.12, delta.z, 0.75f, 2.0f);
        level.addFreshEntity(thrown);
    }

    /** Apply all potion effects from a drinkable potion to the beneficiary. */
    public static void applyDrinkableTo(LivingEntity beneficiary, ItemStack potionStack) {
        if (beneficiary == null || potionStack.isEmpty() || !isDrinkablePotion(potionStack)) {
            return;
        }
        PotionContents contents = potionStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (!(beneficiary.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().value().isInstantaneous()) {
                effect.getEffect().value().applyInstantaneousEffect(serverLevel, null, null, beneficiary, effect.getAmplifier(), 1.0);
            } else {
                beneficiary.addEffect(new MobEffectInstance(effect));
            }
        }
    }
}
