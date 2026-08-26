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
import com.azscompanions.util.NbtUuids;
import com.azscompanions.util.OwnableUuids;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Mister Wiggly perk: a tamed dog that follows his companion (not the player).
 * <p>
 * Name {@link AzsCompanionsConstants#WIGGLY_DOG_NAME}, blue collar â€” based on stream archives
 * referring to his â€œstream dogâ€ / â€œMr Wiggly botâ€ with a blue collar
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

    /** True when this owner currently has at least one living summoned companion. */
    public static boolean hasSummonedCompanion(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        var server = level.getServer();
        if (server == null) {
            return false;
        }
        UUID owner = player.getUUID();
        for (ServerLevel dim : server.getAllLevels()) {
            for (var entity : dim.getAllEntities()) {
                if (entity instanceof CompanionEntity companion
                        && companion.isAlive()
                        && owner.equals(OwnableUuids.get(companion))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Ensure exactly one sidekick dog exists for this companion while summoned. */
    public static void ensureFor(CompanionEntity companion) {
        if (companion.level().isClientSide || !(companion.level() instanceof ServerLevel level)) {
            return;
        }
        UUID owner = OwnableUuids.get(companion);
        if (!isWigglyOwner(owner)) {
            return;
        }
        Wolf existing = findOrCullSidekick(level, companion);
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
        if (!isWigglyOwner(OwnableUuids.get(companion))) {
            return;
        }
        Wolf dog = findOrCullSidekick(level, companion);
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
        var server = level.getServer();
        if (server == null) {
            return;
        }
        UUID follow = companion.getUUID();
        for (ServerLevel dim : server.getAllLevels()) {
            for (var entity : dim.getAllEntities()) {
                if (entity instanceof Wolf wolf && isSidekick(wolf)) {
                    CompoundTag data = wolf.getPersistentData();
                    if (NbtUuids.has(data, TAG_FOLLOW) && follow.equals(NbtUuids.get(data, TAG_FOLLOW))) {
                        wolf.discard();
                    }
                }
            }
        }
    }

    /** Keep one sidekick for this companion across loaded entities; discard extras. */
    private static Wolf findOrCullSidekick(ServerLevel level, CompanionEntity companion) {
        var server = level.getServer();
        if (server == null) {
            return null;
        }
        UUID follow = companion.getUUID();
        List<Wolf> owned = new ArrayList<>();
        for (ServerLevel dim : server.getAllLevels()) {
            for (var entity : dim.getAllEntities()) {
                if (entity instanceof Wolf wolf && wolf.isAlive() && isSidekick(wolf)) {
                    CompoundTag data = wolf.getPersistentData();
                    if (NbtUuids.has(data, TAG_FOLLOW) && follow.equals(NbtUuids.get(data, TAG_FOLLOW))) {
                        owned.add(wolf);
                    }
                }
            }
        }
        if (owned.isEmpty()) {
            return null;
        }
        Wolf keep = WigglyDogPerkSupport.pickOneToKeep(owned, wolf -> {
            double dimPenalty = wolf.level() == level ? 0.0d : 1.0e12d;
            return dimPenalty + wolf.distanceToSqr(companion);
        });
        for (Wolf wolf : owned) {
            if (wolf != keep) {
                wolf.discard();
            }
        }
        return keep;
    }

    private static boolean isSidekick(Wolf wolf) {
        return wolf.getPersistentData().getBooleanOr(TAG_SIDEKICK, false);
    }

    private static void spawnSidekick(ServerLevel level, CompanionEntity companion, UUID ownerUuid) {
        Wolf wolf = EntityType.WOLF.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (wolf == null) {
            return;
        }
        wolf.snapTo(companion.getX() + 0.8d, companion.getY(), companion.getZ() + 0.8d,
                companion.getYRot(), 0.0f);
        wolf.setPersistenceRequired();
        wolf.setTame(true, true);
        if (companion.getOwner() instanceof ServerPlayer player) {
            OwnableUuids.set(wolf, player.getUUID());
        } else {
            OwnableUuids.set(wolf, ownerUuid);
        }
        wolf.setCustomName(Component.literal(AzsCompanionsConstants.WIGGLY_DOG_NAME));
        wolf.setCustomNameVisible(true);
        wolf.setOrderedToSit(false);
        // Collar color API is private in 1.21.1 â€” set via NBT (BLUE = 11).
        var nbt = new CompoundTag();
        wolf.saveWithoutId(nbt);
        nbt.putByte("CollarColor", (byte) DyeColor.BLUE.getId());
        wolf.load(nbt);
        wolf.getPersistentData().putBoolean(TAG_SIDEKICK, true);
        NbtUuids.put(wolf.getPersistentData(), TAG_FOLLOW, companion.getUUID());
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
                    SpecialPlayerPerks.safeTeleportBeside(wolf, target, 2.5d);
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
