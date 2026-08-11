package com.azscompanions.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

public final class FabricCharmData {
    public static final String TAG_BOUND = "BoundCompanion";
    public static final String TAG_STORED = "StoredCompanion";
    public static final String TAG_BED_GRANTED = "KonBedGranted";
    /** @see com.azscompanions.entity.CompanionLogoutPersistence#CHARM_LOGOUT_PARKED */
    public static final String TAG_LOGOUT_PARKED = com.azscompanions.entity.CompanionLogoutPersistence.CHARM_LOGOUT_PARKED;

    private FabricCharmData() {
    }

    public static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null ? data.copyTag() : new CompoundTag();
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean isBound(ItemStack stack) {
        return getBoundUuid(stack) != null;
    }

    public static boolean hasStoredCompanion(ItemStack stack) {
        return getTag(stack).contains(TAG_STORED);
    }

    public static boolean hasGrantedBed(ItemStack stack) {
        return getTag(stack).getBoolean(TAG_BED_GRANTED);
    }

    public static void markBedGranted(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        tag.putBoolean(TAG_BED_GRANTED, true);
        setTag(stack, tag);
    }

    public static UUID getBoundUuid(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        return tag.hasUUID(TAG_BOUND) ? tag.getUUID(TAG_BOUND) : null;
    }

    public static void bind(ItemStack stack, UUID companionUuid) {
        CompoundTag tag = getTag(stack);
        tag.putUUID(TAG_BOUND, companionUuid);
        setTag(stack, tag);
    }

    public static void storeCompanion(ItemStack stack, CompoundTag entityTag, UUID companionUuid) {
        CompoundTag tag = getTag(stack);
        tag.putUUID(TAG_BOUND, companionUuid);
        tag.put(TAG_STORED, entityTag);
        tag.remove(TAG_LOGOUT_PARKED);
        setTag(stack, tag);
    }

    /** Logout parking: store entity NBT and mark for auto-restore on the next join. */
    public static void storeCompanionForLogout(ItemStack stack, CompoundTag entityTag, UUID companionUuid) {
        CompoundTag tag = getTag(stack);
        tag.putUUID(TAG_BOUND, companionUuid);
        tag.put(TAG_STORED, entityTag);
        tag.putBoolean(TAG_LOGOUT_PARKED, true);
        setTag(stack, tag);
    }

    public static boolean isLogoutParked(ItemStack stack) {
        return getTag(stack).getBoolean(TAG_LOGOUT_PARKED);
    }

    public static void clearLogoutParked(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(TAG_LOGOUT_PARKED)) {
            return;
        }
        tag.remove(TAG_LOGOUT_PARKED);
        setTag(stack, tag);
    }

    /** Read stored companion NBT without removing it (safe if spawn fails). */
    public static CompoundTag peekStoredCompanion(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(TAG_STORED)) {
            return null;
        }
        return tag.getCompound(TAG_STORED);
    }

    public static void clearStoredCompanion(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        tag.remove(TAG_STORED);
        tag.remove(TAG_LOGOUT_PARKED);
        setTag(stack, tag);
    }

    public static CompoundTag takeStoredCompanion(ItemStack stack) {
        CompoundTag stored = peekStoredCompanion(stack);
        if (stored == null) {
            return null;
        }
        clearStoredCompanion(stack);
        return stored;
    }
}
