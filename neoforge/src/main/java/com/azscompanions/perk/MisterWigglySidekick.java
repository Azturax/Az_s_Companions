package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Mister Wiggly perk: a tamed dog that follows his companion (not the player).
 * <p>
 * Name {@link AzsCompanionsConstants#WIGGLY_DOG_NAME}, blue collar — based on stream archives
 * referring to his “stream dog” / “Mr Wiggly bot” with a blue collar
 * (Twitch/YouTube archive descriptions for misterwiggly).
 */
public final class MisterWigglySidekick {
    public static final String TAG_SIDEKICK = "azscompanions_wiggly_sidekick";
    public static final String TAG_FOLLOW = "azscompanions_follow_companion";

    /**
     * At companion default scale ({@link CompanionEntity#DEFAULT_BODY_SCALE} = 0.7),
     * dog uses vanilla wolf scale 1.0: {@code dogScale = companionScale * factor}.
     */
    public static final float DOG_SCALE_FACTOR = 1.0f / CompanionEntity.DEFAULT_BODY_SCALE;

    private MisterWigglySidekick() {
    }

    public static boolean isWigglyOwner(UUID ownerUuid) {
        return ownerUuid != null && AzsCompanionsConstants.MISTER_WIGGLY_PLAYER_UUID.equals(ownerUuid);
    }

    /** Ensure exactly one sidekick dog exists for this companion while summoned. */
    public static void ensureFor(CompanionEntity companion) {
        if (companion.level().isClientSide || !(companion.level() instanceof ServerLevel level)) {
            return;
        }
        UUID owner = companion.getOwnerUuid();
        if (!isWigglyOwner(owner)) {
            return;
        }
        Wolf existing = findSidekickNear(level, companion);
        if (existing != null && existing.isAlive()) {
            existing.setOrderedToSit(false);
            syncScale(existing, companion);
            return;
        }
        spawnSidekick(level, companion, owner);
    }

    /** Keep dog {@link Attributes#SCALE} proportional to the companion size slider. */
    public static void syncScaleFromCompanion(CompanionEntity companion) {
        if (companion.level().isClientSide || !(companion.level() instanceof ServerLevel level)) {
            return;
        }
        if (!isWigglyOwner(companion.getOwnerUuid())) {
            return;
        }
        Wolf dog = findSidekickNear(level, companion);
        if (dog != null && dog.isAlive()) {
            syncScale(dog, companion);
        }
    }

    private static void syncScale(Wolf dog, CompanionEntity companion) {
        float dogScale = companion.getBodyScale() * DOG_SCALE_FACTOR;
        dogScale = Math.max(CompanionEntity.MIN_BODY_SCALE, Math.min(CompanionEntity.MAX_BODY_SCALE, dogScale));
        var attr = dog.getAttribute(Attributes.SCALE);
        if (attr == null) {
            return;
        }
        if (Math.abs(attr.getBaseValue() - dogScale) > 0.001d) {
            attr.setBaseValue(dogScale);
            dog.refreshDimensions();
        }
    }

    /** Remove the dog when the companion is stored or removed. */
    public static void despawnFor(CompanionEntity companion) {
        if (companion.level().isClientSide || !(companion.level() instanceof ServerLevel level)) {
            return;
        }
        AABB box = companion.getBoundingBox().inflate(96.0d);
        List<Wolf> nearby = level.getEntitiesOfClass(Wolf.class, box, MisterWigglySidekick::isSidekick);
        for (Wolf wolf : nearby) {
            CompoundTag data = wolf.getPersistentData();
            if (data.hasUUID(TAG_FOLLOW) && companion.getUUID().equals(data.getUUID(TAG_FOLLOW))) {
                wolf.discard();
            }
        }
    }

    private static Wolf findSidekickNear(ServerLevel level, CompanionEntity companion) {
        AABB box = companion.getBoundingBox().inflate(96.0d);
        List<Wolf> nearby = level.getEntitiesOfClass(Wolf.class, box, MisterWigglySidekick::isSidekick);
        for (Wolf wolf : nearby) {
            CompoundTag data = wolf.getPersistentData();
            if (data.hasUUID(TAG_FOLLOW) && companion.getUUID().equals(data.getUUID(TAG_FOLLOW))) {
                return wolf;
            }
        }
        return null;
    }

    private static boolean isSidekick(Wolf wolf) {
        return wolf.getPersistentData().getBoolean(TAG_SIDEKICK);
    }

    private static void spawnSidekick(ServerLevel level, CompanionEntity companion, UUID ownerUuid) {
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf == null) {
            return;
        }
        wolf.moveTo(companion.getX() + 0.8d, companion.getY(), companion.getZ() + 0.8d,
                companion.getYRot(), 0.0f);
        wolf.setPersistenceRequired();
        wolf.setTame(true, true);
        if (companion.getOwner() instanceof ServerPlayer player) {
            wolf.setOwnerUUID(player.getUUID());
        } else {
            wolf.setOwnerUUID(ownerUuid);
        }
        wolf.setCustomName(Component.literal(AzsCompanionsConstants.WIGGLY_DOG_NAME));
        wolf.setCustomNameVisible(true);
        wolf.setOrderedToSit(false);
        // Collar color API is private in 1.21.1 — set via NBT (BLUE = 11).
        var nbt = new CompoundTag();
        wolf.saveWithoutId(nbt);
        nbt.putByte("CollarColor", (byte) DyeColor.BLUE.getId());
        wolf.load(nbt);
        wolf.getPersistentData().putBoolean(TAG_SIDEKICK, true);
        wolf.getPersistentData().putUUID(TAG_FOLLOW, companion.getUUID());
        wolf.goalSelector.addGoal(2, new FollowCompanionEntityGoal(wolf, companion.getUUID()));
        syncScale(wolf, companion);
        level.addFreshEntity(wolf);
    }

    /** Follow a companion entity by UUID (companion-of-companion). */
    public static final class FollowCompanionEntityGoal extends Goal {
        private final Wolf wolf;
        private final UUID companionUuid;
        private CompanionEntity target;
        private int recalc;

        public FollowCompanionEntityGoal(Wolf wolf, UUID companionUuid) {
            this.wolf = wolf;
            this.companionUuid = companionUuid;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            target = resolve();
            return target != null && target.isAlive() && wolf.distanceTo(target) > 2.5d;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && wolf.distanceTo(target) > 1.8d;
        }

        @Override
        public void start() {
            recalc = 0;
            wolf.setOrderedToSit(false);
        }

        @Override
        public void stop() {
            wolf.getNavigation().stop();
            target = null;
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            wolf.getLookControl().setLookAt(target, 10.0f, wolf.getMaxHeadXRot());
            if (--recalc <= 0) {
                recalc = 10;
                double dist = wolf.distanceTo(target);
                if (dist > 24.0d) {
                    wolf.teleportTo(target.getX(), target.getY(), target.getZ());
                } else {
                    wolf.getNavigation().moveTo(target, 1.15d);
                }
            }
        }

        private CompanionEntity resolve() {
            if (!(wolf.level() instanceof ServerLevel level)) {
                return null;
            }
            var entity = level.getEntity(companionUuid);
            return entity instanceof CompanionEntity c ? c : null;
        }
    }
}
