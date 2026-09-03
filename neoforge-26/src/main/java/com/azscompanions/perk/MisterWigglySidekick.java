package com.azscompanions.perk;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
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

    private MisterWigglySidekick() {
    }

    public static boolean isWigglyOwner(UUID ownerUuid) {
        return ownerUuid != null && AzsCompanionsConstants.MISTER_WIGGLY_PLAYER_UUID.equals(ownerUuid);
    }

    static boolean qualifies(CompanionEntity companion) {
        return companion != null && WigglyDogPerkSupport.shouldSpawnCompanionSidekick(
                companion.getOwnerUuid(),
                companion.isCciSummoned(),
                companion.isChildCompanion(),
                companion.getDefinition().id().toString(),
                companion.getForm().serializedName());
    }

    /** True when a charm-owned Wiggly companion is out (sidekick should own the dog slot). */
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
                        && owner.equals(companion.getOwnerUuid())
                        && qualifies(companion)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Ensure exactly one sidekick dog exists for a charm Wiggly companion. */
    public static void ensureFor(CompanionEntity companion) {
        if (companion.level().isClientSide() || !(companion.level() instanceof ServerLevel level)) {
            return;
        }
        if (!qualifies(companion)) {
            despawnFor(companion);
            return;
        }
        UUID owner = companion.getOwnerUuid();
        Wolf existing = findOrCullSidekick(level, companion);
        if (existing != null && existing.isAlive()) {
            existing.setOrderedToSit(false);
            syncScale(existing);
            return;
        }
        spawnSidekick(level, companion, owner);
    }

    /** Keep dog {@link Attributes#SCALE} at {@link WigglyDogPerkSupport#DOG_SCALE}. */
    public static void syncScaleFromCompanion(CompanionEntity companion) {
        if (companion.level().isClientSide() || !(companion.level() instanceof ServerLevel level)) {
            return;
        }
        if (!qualifies(companion)) {
            return;
        }
        Wolf dog = findOrCullSidekick(level, companion);
        if (dog != null && dog.isAlive()) {
            syncScale(dog);
        }
    }

    private static void syncScale(Wolf dog) {
        var attr = dog.getAttribute(Attributes.SCALE);
        if (attr == null) {
            return;
        }
        if (WigglyDogPerkSupport.scaleNeedsUpdate(attr.getBaseValue())) {
            attr.setBaseValue(WigglyDogPerkSupport.DOG_SCALE);
            dog.refreshDimensions();
        }
    }

    /** Remove the dog when the companion is stored or removed. */
    public static void despawnFor(CompanionEntity companion) {
        if (companion.level().isClientSide() || !(companion.level() instanceof ServerLevel level)) {
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
                    if (data.read(TAG_FOLLOW, UUIDUtil.CODEC).isPresent()
                            && follow.equals(data.read(TAG_FOLLOW, UUIDUtil.CODEC).orElse(null))) {
                        wolf.discard();
                    }
                }
            }
        }
    }

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
                    owned.add(wolf);
                }
            }
        }
        if (owned.isEmpty()) {
            return null;
        }
        Wolf keep = WigglyDogPerkSupport.pickOneToKeep(owned, wolf -> {
            CompoundTag data = wolf.getPersistentData();
            UUID followTarget = data.read(TAG_FOLLOW, UUIDUtil.CODEC).orElse(null);
            double followBonus = follow.equals(followTarget) ? -1.0e15d : 0.0d;
            double dimPenalty = wolf.level() == level ? 0.0d : 1.0e12d;
            return followBonus + dimPenalty + wolf.distanceToSqr(companion);
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
        Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (wolf == null) {
            return;
        }
        wolf.snapTo(companion.getX() + 0.8d, companion.getY(), companion.getZ() + 0.8d,
                companion.getYRot(), 0.0f);
        wolf.setPersistenceRequired();
        wolf.setTame(true, true);
        if (companion.getOwner() instanceof ServerPlayer player) {
            wolf.setOwner(player);
        } else {
            // Owner living entity unavailable offline; tame flag still set above.
        }
        wolf.setCustomName(Component.literal(AzsCompanionsConstants.WIGGLY_DOG_NAME));
        wolf.setCustomNameVisible(true);
        wolf.setOrderedToSit(false);
        wolf.getPersistentData().putBoolean(TAG_SIDEKICK, true);
        wolf.getPersistentData().store(TAG_FOLLOW, UUIDUtil.CODEC, companion.getUUID());
        wolf.goalSelector.addGoal(2, new FollowCompanionEntityGoal(wolf, companion.getUUID()));
        syncScale(wolf);
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
