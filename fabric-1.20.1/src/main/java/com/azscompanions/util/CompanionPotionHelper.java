package com.azscompanions.util;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
        boolean beneficial = false;
        boolean harmful = false;
        boolean any = false;
        for (MobEffectInstance effect : effects) {
            any = true;
            MobEffect mobEffect = effect.getEffect();
            MobEffectCategory category = mobEffect.getCategory();
            if (category == MobEffectCategory.BENEFICIAL) {
                beneficial = true;
            } else if (category == MobEffectCategory.HARMFUL) {
                harmful = true;
            }
        }
        if (!any) {
            return Kind.NEUTRAL;
        }
        if (harmful) {
            return Kind.HARMFUL;
        }
        if (beneficial) {
            return Kind.BENEFICIAL;
        }
        return Kind.NEUTRAL;
    }

    public static void throwPotionAt(LivingEntity thrower, ItemStack potionStack, LivingEntity target) {
        Level level = thrower.level();
        if (level.isClientSide || target == null || potionStack.isEmpty()) {
            return;
        }
        ThrownPotion thrown = new ThrownPotion(level, thrower);
        thrown.setItem(ItemStackCompat.copyWithCount(potionStack, 1));
        Vec3 from = thrower.getEyePosition();
        Vec3 to = target.getEyePosition().add(0.0, target.getBbHeight() * 0.15, 0.0);
        Vec3 delta = to.subtract(from);
        thrown.shoot(delta.x, delta.y + 0.12, delta.z, 0.75f, 2.0f);
        level.addFreshEntity(thrown);
    }

    public static void applyDrinkableTo(LivingEntity beneficiary, ItemStack potionStack) {
        if (beneficiary == null || potionStack.isEmpty() || !isDrinkablePotion(potionStack)) {
            return;
        }
        for (MobEffectInstance effect : PotionUtils.getMobEffects(potionStack)) {
            MobEffect mobEffect = effect.getEffect();
            if (mobEffect.isInstantenous()) {
                mobEffect.applyInstantenousEffect(null, null, beneficiary, effect.getAmplifier(), 1.0);
            } else {
                beneficiary.addEffect(new MobEffectInstance(effect));
            }
        }
    }
}
