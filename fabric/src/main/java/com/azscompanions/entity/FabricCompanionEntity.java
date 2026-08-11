package com.azscompanions.entity;

import com.azscompanions.ai.CompanionAiActionTrust;
import com.azscompanions.ai.CompanionAiChatSupport;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.ai.CompanionChatCensor;
import com.azscompanions.ai.CompanionRecentAction;
import com.azscompanions.ai.CompanionRecentActionMemory;
import com.azscompanions.ai.FabricCompanionAiAsk;
import com.azscompanions.compat.hosted.IntegratedMultiplayerCompat;
import com.azscompanions.compat.hosted.PlayerIdentityCompat;
import com.azscompanions.config.FabricServerConfig;
import com.azscompanions.entity.inventory.FabricCompanionInventory;
import com.azscompanions.menu.FabricCompanionInventoryMenu;
import com.azscompanions.network.FabricNetworking;
import com.azscompanions.perk.SpecialPlayerPerks;
import com.azscompanions.perk.WolfyPerkSupport;
import com.azscompanions.item.FabricCompanionCharmItem;
import com.azscompanions.registry.FabricModItems;
import com.azscompanions.task.FabricTaskQueue;
import com.azscompanions.util.CompanionArmorRules;
import com.azscompanions.world.FabricCompanionChunkTickets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class FabricCompanionEntity extends PathfinderMob {
    public static final float MIN_BODY_SCALE = 0.5f;
    public static final float MAX_BODY_SCALE = 3.0f;
    public static final float DEFAULT_BODY_SCALE = 0.7f;

    private static final EntityDataAccessor<String> DATA_DEFINITION =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_MODE =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_NAME =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_BODY_SCALE =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_SKIN_PATH =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN_SLEEPING =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN_BATHING =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN_ADVENTURING =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ACTIVE_CONTEXT =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SLIM =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_GENDER =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_BUST =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WAIST =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HIPS =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHOULDERS =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BUST_OFFSET =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_FORM =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_FORM_VARIANT =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SHOW_NAME_TAG =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHOW_ARMOR =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_ATTITUDE =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TEAM =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_FOLLOW_RADIUS =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PERSONAL_SPACE =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WANDER_RADIUS =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.FLOAT);
    /** Synced so client UI ownership checks work without looking at NBT. */
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_STORED_CHILD_COUNT =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MAX_CHILDREN =
            SynchedEntityData.defineId(FabricCompanionEntity.class, EntityDataSerializers.INT);

    private final FabricCompanionInventory inventory = new FabricCompanionInventory();
    private final ListTag storedChildren = new ListTag();
    private final FabricTaskQueue taskQueue = new FabricTaskQueue(this);
    private final OwnerActivityTracker ownerActivity = new OwnerActivityTracker();
    private final Set<UUID> trusted = new HashSet<>();
    private BlockPos homePos;
    private BlockPos homeBedPos;
    private String voiceProfile = "kon_soft";
    /** Last-known owner profile name (NBT); hosted-world UUID remap fallback. */
    private String ownerName = "";
    private boolean konBedGranted;
    /** Transient playful “turn evil” countdown (ticks). Not persisted. */
    private int playfulEvilTicks;
    /** Duration set at the start of the current playful-evil burst. */
    private int playfulEvilDurationTicks;
    private CompanionAttitude playfulEvilRestoreAttitude = CompanionAttitude.PASSIVE;
    private UUID leaderUuid;
    private boolean fightSpawn;
    /** Game tick when next ambient idle chat may fire (0 = schedule on first opportunity). */
    private int nextIdleChatTick;
    /** Continuous ticks the owner has been beyond callPlayerDistance. */
    private int ownerAwayTicks;
    /** Game tick when last call-player line was spoken. */
    private int lastCallPlayerTick = Integer.MIN_VALUE / 4;
    /** Game tick of last speakLine (owner chat); used to space ambient chatter. */
    private int lastSpeakTick;
    private CompanionPlayMode playMode = CompanionPlayMode.NONE;
    private int playTicksRemaining;
    private BlockPos playHideTarget;
    /** Per-companion AI persona (who / what / how). Persisted in NBT; not synched. */
    private com.azscompanions.ai.CompanionPersona persona = com.azscompanions.ai.CompanionPersona.EMPTY;
    /**
     * Per-companion chunk ticket opt-out. When false, no entity ticket even if server
     * {@code companionChunkLoading} is on. Default true.
     */
    private boolean chunkLoadingEnabled = true;
    /** Pending flower the companion offers after a gift; empty when none. Persisted. */
    private ItemStack offeredFlower = ItemStack.EMPTY;
    /** Game time when the next flower gift is allowed. Not persisted. */
    private long flowerGiftCooldownUntil;
    /** Mounted via owner ride-along; sync-dismount when the owner dismounts. Not persisted. */
    private boolean rideAlongActive;

    /** Default playful-evil duration when no CCI {@code seconds=} is given. */
    public static final int PLAYFUL_EVIL_DEFAULT_SECONDS = 10;

    public FabricCompanionEntity(EntityType<? extends FabricCompanionEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanFloat(true);
        }
        // Allow pathing through water when following a swimming owner (default WATER malus is high).
        this.setPathfindingMalus(PathType.WATER, 0.0f);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0d)
                .add(Attributes.MOVEMENT_SPEED, 0.32d)
                .add(Attributes.ATTACK_DAMAGE, CompanionCombatDamage.NETHERITE_SWORD_ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 64.0d)
                .add(Attributes.SCALE, DEFAULT_BODY_SCALE)
                // Clear full 1-block steps at any body scale (0.5–3); vanilla step is only 0.6.
                .add(Attributes.STEP_HEIGHT, CompanionMovementAttributes.STEP_HEIGHT)
                .add(Attributes.JUMP_STRENGTH, CompanionMovementAttributes.JUMP_STRENGTH);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        CompanionForm form = getForm();
        return EntityDimensions.scalable(form.width(), form.height());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DEFINITION, FabricCompanionRegistry.KON_ID.toString());
        builder.define(DATA_MODE, FabricCompanionMode.FOLLOW.name());
        builder.define(DATA_NAME, "");
        builder.define(DATA_BODY_SCALE, DEFAULT_BODY_SCALE);
        builder.define(DATA_SKIN_PATH, "");
        builder.define(DATA_SKIN_SLEEPING, "");
        builder.define(DATA_SKIN_BATHING, "");
        builder.define(DATA_SKIN_ADVENTURING, "");
        builder.define(DATA_ACTIVE_CONTEXT, "");
        builder.define(DATA_SLIM, false);
        builder.define(DATA_GENDER, CompanionGender.FEMALE.getSerializedName());
        builder.define(DATA_BUST, CompanionBodyProportions.DEFAULT_BUST);
        builder.define(DATA_WAIST, CompanionBodyProportions.DEFAULT_WAIST);
        builder.define(DATA_HIPS, CompanionBodyProportions.DEFAULT_HIPS);
        builder.define(DATA_SHOULDERS, CompanionBodyProportions.DEFAULT_SHOULDERS);
        builder.define(DATA_BUST_OFFSET, CompanionBodyProportions.DEFAULT_BUST_OFFSET);
        builder.define(DATA_FORM, CompanionForm.PLAYER.serializedName());
        builder.define(DATA_FORM_VARIANT, "");
        builder.define(DATA_SHOW_NAME_TAG, true);
        builder.define(DATA_SHOW_ARMOR, true);
        builder.define(DATA_ATTITUDE, CompanionAttitude.PASSIVE.serializedName());
        builder.define(DATA_TEAM, "");
        builder.define(DATA_FOLLOW_RADIUS, CompanionFollowDistances.DEFAULT_FOLLOW_RADIUS);
        builder.define(DATA_PERSONAL_SPACE, CompanionFollowDistances.DEFAULT_PERSONAL_SPACE);
        builder.define(DATA_WANDER_RADIUS, CompanionFollowDistances.DEFAULT_WANDER_RADIUS);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_STORED_CHILD_COUNT, 0);
        builder.define(DATA_MAX_CHILDREN, CompanionChildLimits.MAX_PER_LEADER);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FabricSitGoal(this));
        goalSelector.addGoal(2, new FabricCompanionSleepInBedGoal(this));
        goalSelector.addGoal(3, new FabricPotionBehaviorGoal(this));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.25d, true));
        goalSelector.addGoal(5, new FabricCompanionRideAlongGoal(this));
        goalSelector.addGoal(6, new FabricFollowOwnerGoal(this));
        goalSelector.addGoal(7, new FabricWanderMobInteractGoal(this));
        goalSelector.addGoal(8, new FabricWanderNearOwnerGoal(this));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new FabricOwnerDefendTargetGoal(this));
        targetSelector.addGoal(2, new FabricHostileTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && level() instanceof ServerLevel serverLevel) {
            if (getOwnerUuid() != null
                    && (!isPersistenceRequired()
                            || !getTags().contains(CompanionNoDespawnSupport.ENTITY_TAG))) {
                applyOwnedNoDespawn();
            }
            // Preserve player/CCI command modes; allow TASK while task queue is active.
            {
                FabricCompanionMode mode = getMode();
                if (mode != FabricCompanionMode.FOLLOW
                        && mode != FabricCompanionMode.SIT
                        && mode != FabricCompanionMode.STAY
                        && mode != FabricCompanionMode.WANDER
                        && mode != FabricCompanionMode.TASK) {
                    setMode(FabricCompanionMode.FOLLOW);
                }
            }
            taskQueue.tick(serverLevel);
            if (getMode() == FabricCompanionMode.TASK) {
                com.azscompanions.util.FabricToolSelectionHelper.preferTorchOffhand(this, true);
            }
            tickOwnerActivity();
            tickContextSkinState();
            SpecialPlayerPerks.applyCompanionPerks(this, getOwnerUuid());
            tickSleepPurr();
            tickHomeBedLeash();
            tickPlayfulEvil();
            tickAiAmbientSpeech();
            tickPlayBehavior();
            tickChildParentLeash();
            tickRideAlongSync();
            // Follow-only ground leash — never during Wander stroll / home-idle.
            if (getMode() == FabricCompanionMode.FOLLOW
                    && shouldActivelyFollowOwner()
                    && isOwnerExploring()
                    && !isPassenger()
                    && (getTarget() == null || !getTarget().isAlive())) {
                Player owner = getOwner();
                if (owner != null) {
                    double dist = distanceTo(owner);
                    if (!CompanionFollowDistances.tooCloseToTeleport(dist, getFollowRadius())
                            && CompanionFollowDistances.shouldGroundTeleport(dist, getFollowRadius())) {
                        safeTeleportNearOwner(owner);
                    }
                }
            }
            FabricCompanionChunkTickets.tick(this, serverLevel);
            if (tickCount % 20 == 0) {
                ejectForbiddenCharm();
            }
        }
    }

    private void tickHomeBedLeash() {
        FabricCompanionMode mode = getMode();
        if (mode == FabricCompanionMode.STAY || mode == FabricCompanionMode.SIT) {
            return;
        }
        if (mode != FabricCompanionMode.FOLLOW && mode != FabricCompanionMode.WANDER) {
            return;
        }
        if (getTarget() != null && getTarget().isAlive()) {
            return;
        }
        if (isSleeping()) {
            return;
        }
        if (isPassenger()) {
            return;
        }
        Player owner = getOwner();
        BlockPos bed = getHomeBedPos();
        if (owner == null || bed == null) {
            return;
        }
        double radius = FabricServerConfig.HOME_BED_RADIUS;
        if (owner.distanceToSqr(bed.getX() + 0.5d, bed.getY(), bed.getZ() + 0.5d) <= radius * radius) {
            return;
        }
        double dist = distanceTo(owner);
        if (CompanionFollowDistances.tooCloseToTeleport(dist, getFollowRadius())) {
            return;
        }
        safeTeleportNearOwner(owner);
    }

    public void safeTeleportNearOwner(Player owner) {
        safeTeleportNear(owner);
    }

    public void safeTeleportNear(net.minecraft.world.entity.Entity target) {
        if (isPassenger()) {
            return;
        }
        FabricCompanionMode mode = getMode();
        if (mode == FabricCompanionMode.STAY || mode == FabricCompanionMode.SIT) {
            return;
        }
        net.minecraft.world.phys.Vec3 away = position().subtract(target.position());
        if (away.horizontalDistanceSqr() < 1.0e-4d) {
            away = new net.minecraft.world.phys.Vec3(1.0d, 0.0d, 0.0d);
        }
        net.minecraft.world.phys.Vec3 offset = new net.minecraft.world.phys.Vec3(away.x, 0.0d, away.z)
                .normalize()
                .scale(CompanionFollowDistances.preferredDistance(getPersonalSpace()));
        teleportTo(target.getX() + offset.x, target.getY(), target.getZ() + offset.z);
    }

    public double getHomeBedRadius() {
        return FabricServerConfig.HOME_BED_RADIUS;
    }

    public boolean isNearHomeBed() {
        BlockPos bed = getHomeBedPos();
        if (bed == null) {
            return false;
        }
        double r = getHomeBedRadius();
        return distanceToSqr(bed.getX() + 0.5d, bed.getY(), bed.getZ() + 0.5d) <= r * r;
    }

    public boolean isOwnerFarFromHomeBed() {
        Player owner = getOwner();
        BlockPos bed = getHomeBedPos();
        if (owner == null || bed == null) {
            return false;
        }
        double r = getHomeBedRadius();
        return owner.distanceToSqr(bed.getX() + 0.5d, bed.getY(), bed.getZ() + 0.5d) > r * r;
    }

    public boolean shouldActivelyFollowOwner() {
        FabricCompanionMode mode = getMode();
        if (mode == FabricCompanionMode.STAY || mode == FabricCompanionMode.SIT) {
            return false;
        }
        if (mode == FabricCompanionMode.WANDER) {
            return getHomeBedPos() != null && isOwnerFarFromHomeBed();
        }
        if (mode != FabricCompanionMode.FOLLOW) {
            return false;
        }
        if (getHomeBedPos() == null) {
            return true;
        }
        if (isOwnerFarFromHomeBed()) {
            return true;
        }
        return !isNearHomeBed();
    }

    public boolean isRideAlongActive() {
        return rideAlongActive;
    }

    public void setRideAlongActive(boolean active) {
        this.rideAlongActive = active;
    }

    /** Dismount when ride-along mounted us and the owner is no longer riding. */
    private void tickRideAlongSync() {
        Player owner = getOwner();
        boolean ownerRiding = owner != null && owner.isPassenger();
        if (CompanionRideAlongSupport.shouldSyncDismount(rideAlongActive, ownerRiding)) {
            CompanionRideAlong.stopRideAlong(this);
            rideAlongActive = false;
        }
    }

    public boolean shouldHomeIdleNearBed() {
        FabricCompanionMode mode = getMode();
        if (mode == FabricCompanionMode.STAY || mode == FabricCompanionMode.SIT) {
            return false;
        }
        if (getHomeBedPos() == null || isOwnerFarFromHomeBed()) {
            return false;
        }
        return isNearHomeBed() && (mode == FabricCompanionMode.FOLLOW || mode == FabricCompanionMode.WANDER);
    }

    private void tickOwnerActivity() {
        Player owner = getOwner();
        if (owner == null) {
            ownerActivity.reset();
            return;
        }
        ownerActivity.tick(owner.getX(), owner.getZ());
    }

    private void tickContextSkinState() {
        boolean bathing = CompanionContextSkinSupport.isBathing(isSleeping(), isInWaterOrBubble());
        CompanionContextSkinSupport.Context active = CompanionContextSkinSupport.activeContext(
                getForm().isPlayer(), isSleeping(), bathing, isOwnerExploring());
        String id = active == null ? "" : active.id();
        if (!id.equals(entityData.get(DATA_ACTIVE_CONTEXT))) {
            entityData.set(DATA_ACTIVE_CONTEXT, id);
        }
    }

    /**
     * Idle ambient lines + call-when-away.
     * Prefers LLM when the server provider is enabled; otherwise sparse scripted fallbacks when idleChat is on.
     * Rate-limited; skips combat/sleep and when the shared AI worker is busy (LLM path).
     */
    private void tickAiAmbientSpeech() {
        if (tickCount % 20 != 0) {
            return;
        }
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        CompanionAiSettings settings = runtime.settings();
        if (!settings.idleChat() && !settings.callPlayerWhenAway()) {
            return;
        }
        if (!(getOwner() instanceof ServerPlayer owner) || !owner.isAlive()) {
            ownerAwayTicks = 0;
            return;
        }
        if (isSleeping() || (getTarget() != null && getTarget().isAlive())) {
            return;
        }
        long gameTime = level() instanceof ServerLevel slGt ? slGt.getGameTime() : tickCount;
        boolean hasReactive = CompanionRecentActionMemory.hasReactive(owner.getUUID(), gameTime);
        int speakCoolSec = hasReactive ? 25 : 45;
        // Avoid stacking on top of a recent /ask or ambient line (shorter for reactions).
        if (lastSpeakTick > 0 && CompanionAiChatSupport.spokeTooRecently(tickCount - lastSpeakTick, speakCoolSec)) {
            return;
        }
        String ownerName = owner.getGameProfile().getName();
        double dist = distanceTo(owner);
        double callDist = settings.callPlayerDistance();
        boolean llmOn = runtime.isEnabled();

        if (settings.callPlayerWhenAway()) {
            if (dist > callDist) {
                ownerAwayTicks = Math.min(ownerAwayTicks + 20, settings.callPlayerAfterSeconds() * 20 + 40);
            } else {
                ownerAwayTicks = 0;
            }
            int need = settings.callPlayerAfterSeconds() * 20;
            int cool = settings.callPlayerCooldownSeconds() * 20;
            if (ownerAwayTicks >= need
                    && tickCount - lastCallPlayerTick >= cool
                    && !(llmOn && runtime.isBusy())) {
                lastCallPlayerTick = tickCount;
                ownerAwayTicks = 0;
                String fallback = CompanionAiChatSupport.fallbackCallLine(ownerName);
                if (llmOn) {
                    String prompt = CompanionAiChatSupport.callPlayerPrompt(ownerName);
                    if (!FabricCompanionAiAsk.askQuiet(owner, this, ownerName, prompt,
                            CompanionAiActionTrust.OWNER, null, fallback)) {
                        speakLine(fallback);
                    }
                } else {
                    speakLine(fallback);
                }
                if (getMode() == FabricCompanionMode.FOLLOW) {
                    getNavigation().moveTo(owner, 1.15d);
                }
                return;
            }
        } else {
            ownerAwayTicks = 0;
        }

        if (!settings.idleChat() || (llmOn && runtime.isBusy())) {
            return;
        }
        boolean child = getLeaderUuid() != null;
        double idleMul = child ? settings.childAutonomy().idleIntervalMultiplier() : 1.0d;
        if (dist > settings.chatReactRange()) {
            return;
        }
        CompanionRecentAction focus = hasReactive
                ? CompanionRecentActionMemory.consumeReactive(owner.getUUID(), gameTime).orElse(null)
                : null;
        boolean reactiveNow = focus != null;
        if (!reactiveNow) {
            if (nextIdleChatTick <= 0) {
                int secs = (int) (CompanionAiChatSupport.nextIdleIntervalSeconds(
                        settings.idleChatSecondsMin(), settings.idleChatSecondsMax(), random::nextInt) * idleMul);
                nextIdleChatTick = tickCount + Math.max(40, secs * 20);
                return;
            }
            if (tickCount < nextIdleChatTick) {
                return;
            }
        }
        int secs = (int) (CompanionAiChatSupport.nextIdleIntervalSeconds(
                settings.idleChatSecondsMin(), settings.idleChatSecondsMax(), random::nextInt) * idleMul);
        nextIdleChatTick = tickCount + Math.max(40, secs * 20);
        var recent = CompanionRecentActionMemory.peek(owner.getUUID(), gameTime);
        String fallback = focus != null
                ? CompanionAiChatSupport.fallbackReactiveLine(ownerName, focus)
                : CompanionAiChatSupport.fallbackIdleLine(ownerName);
        if (!llmOn) {
            speakLine(fallback);
            return;
        }
        String prompt;
        if (child && settings.childAutonomy().prefersTalkToParent() && level() instanceof ServerLevel sl
                && sl.getEntity(getLeaderUuid()) instanceof FabricCompanionEntity parent) {
            prompt = "[ambient child] Talk briefly to your parent " + parent.getChatDisplayName()
                    + " — one short wholesome line.";
        } else {
            prompt = CompanionAiChatSupport.ambientPromptWithRecent(ownerName, focus, recent);
        }
        if (!FabricCompanionAiAsk.askQuiet(owner, this, ownerName, prompt,
                CompanionAiActionTrust.OWNER, null, fallback)) {
            speakLine(fallback);
        }
    }

    public void startPlay(CompanionPlayMode mode, int ticks) {
        this.playMode = mode == null ? CompanionPlayMode.NONE : mode;
        this.playTicksRemaining = Math.max(1, ticks);
        this.playHideTarget = null;
    }

    public void clearPlayMode() {
        this.playMode = CompanionPlayMode.NONE;
        this.playTicksRemaining = 0;
        this.playHideTarget = null;
    }

    public CompanionPlayMode getPlayMode() {
        return playMode;
    }

    private void tickPlayBehavior() {
        if (playMode == CompanionPlayMode.NONE || playTicksRemaining <= 0) {
            if (playMode != CompanionPlayMode.NONE) {
                clearPlayMode();
            }
            return;
        }
        playTicksRemaining--;
        Player owner = getOwner();
        switch (playMode) {
            case RUN_AT_PLAYER -> {
                if (owner != null) {
                    getNavigation().moveTo(owner, 1.35d);
                }
            }
            case SEEK -> {
                if (owner != null) {
                    getNavigation().moveTo(owner, 1.2d);
                }
            }
            case HIDE -> {
                if (playHideTarget == null) {
                    playHideTarget = new BlockPos(
                            getBlockX() + random.nextInt(11) - 5,
                            getBlockY(),
                            getBlockZ() + random.nextInt(11) - 5);
                }
                if (blockPosition().distManhattan(playHideTarget) > 2) {
                    getNavigation().moveTo(playHideTarget.getX() + 0.5, playHideTarget.getY(),
                            playHideTarget.getZ() + 0.5, 1.15d);
                } else {
                    setMode(FabricCompanionMode.SIT);
                    getNavigation().stop();
                }
            }
            case DANCE -> {
                setYRot(getYRot() + 25.0f);
                yBodyRot = getYRot();
                getNavigation().stop();
            }
            case PEEKABOO -> {
                if (playTicksRemaining > 20) {
                    setMode(FabricCompanionMode.SIT);
                    getNavigation().stop();
                } else {
                    setMode(FabricCompanionMode.FOLLOW);
                    if (owner != null) {
                        getNavigation().moveTo(owner, 1.25d);
                    }
                }
            }
            default -> {
            }
        }
        if (playTicksRemaining <= 0) {
            clearPlayMode();
        }
    }

    private void tickChildParentLeash() {
        UUID leaderId = getLeaderUuid();
        if (leaderId == null || tickCount % 20 != 0) {
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (!(level.getEntity(leaderId) instanceof FabricCompanionEntity parent) || !parent.isAlive()) {
            return;
        }
        CompanionAiSettings settings = CompanionAiRuntime.get().settings();
        double leash = settings.effectiveChildLeashRadius();
        double dist = distanceTo(parent);
        if (dist <= leash) {
            if (settings.childAutonomy() == com.azscompanions.ai.ChildAutonomyMode.CURIOUS
                    && random.nextInt(40) == 0
                    && getMode() == FabricCompanionMode.FOLLOW) {
                double a = random.nextDouble() * Math.PI * 2;
                getNavigation().moveTo(getX() + Math.cos(a) * 2.5, getY(), getZ() + Math.sin(a) * 2.5, 1.05d);
            }
            return;
        }
        setMode(FabricCompanionMode.FOLLOW);
        getNavigation().moveTo(parent, 1.2d);
        if (dist > leash * 2.5d && !CompanionFollowDistances.tooCloseToTeleport(dist)) {
            net.minecraft.world.phys.Vec3 away = position().subtract(parent.position());
            if (away.horizontalDistanceSqr() < 1.0e-4d) {
                away = new net.minecraft.world.phys.Vec3(1.0d, 0.0d, 0.0d);
            }
            net.minecraft.world.phys.Vec3 offset = new net.minecraft.world.phys.Vec3(away.x, 0.0d, away.z)
                    .normalize()
                    .scale(CompanionFollowDistances.PREFERRED_DISTANCE);
            teleportTo(parent.getX() + offset.x, parent.getY(), parent.getZ() + offset.z);
        }
    }

    /** Soft cat purr every few seconds while asleep — Kon-named companions only. */
    private void tickSleepPurr() {
        if (!isSleeping() || !isKonNamed()) {
            return;
        }
        if ((tickCount + getId()) % 100 != 0) {
            return;
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CAT_PURR, SoundSource.NEUTRAL, 0.55f, 0.95f + random.nextFloat() * 0.15f);
    }

    public boolean isOwnerStandingAround() {
        return ownerActivity.isStandingAround();
    }

    public boolean isOwnerExploring() {
        return ownerActivity.isExploring();
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        if (CompanionHazardImmunity.ignores(source.typeHolder().unwrapKey()
                .map(key -> key.location().getPath())
                .orElse(""))) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && (isOwnedBy(player) || isTrusted(player))) {
            return false;
        }
        if (isInvulnerableTo(source)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (!(target instanceof LivingEntity living) || !canAttackTarget(living)) {
            return false;
        }
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            return super.doHurtTarget(target);
        }
        return CompanionCombatDamage.withFixedMeleeDamage(
                attack.getValue(),
                attack.getBaseValue(),
                attack::setBaseValue,
                () -> super.doHurtTarget(target));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || (!isOwnedBy(player) && !isTrusted(player))) {
            if (!level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.azscompanions.not_owner"), true);
            }
            return InteractionResult.CONSUME;
        }
        // Hold charm + Shift + right-click opens shared menu (Customize | Command | Inventory).
        if (player.isShiftKeyDown()) {
            ItemStack heldForMenu = player.getItemInHand(hand);
            if (FabricCompanionCharmItem.isCharm(heldForMenu)) {
                FabricNetworking.openMenu(serverPlayer, this);
                return InteractionResult.CONSUME;
            }
            // Shift without charm in this hand: do not open menu or swap items.
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!isChildCompanion() && getStoredChildCount() > 0
                && (held.isEmpty() || FabricCompanionCharmItem.isCharm(held))) {
            FabricCompanionEntity called = callNextStoredChild(serverPlayer);
            if (called != null) {
                player.displayClientMessage(Component.translatable(
                        "message.azscompanions.child_called", called.getChatDisplayName()), true);
                return InteractionResult.CONSUME;
            }
            if (getStoredChildCount() > 0) {
                player.displayClientMessage(Component.translatable("message.azscompanions.child_limit_reached"), true);
                return InteractionResult.CONSUME;
            }
        }
        if (!held.isEmpty()) {
            if (FabricCompanionCharmItem.isCharm(held)) {
                return InteractionResult.PASS;
            }
            // Hidden easter egg: fermented spider eye → brief playful HOSTILE burst.
            if (held.is(net.minecraft.world.item.Items.FERMENTED_SPIDER_EYE)) {
                return feedPlayfulEvil(serverPlayer, hand);
            }
            if (held.is(net.minecraft.world.item.Items.CAKE)) {
                return feedCakeSpawnChild(serverPlayer, hand);
            }
            if (CompanionFlowerGift.isFlower(held)) {
                return CompanionFlowerGift.tryGift(this, serverPlayer, hand, held);
            }
            if (isEdibleFood(held)) {
                return feedFromPlayer(serverPlayer, hand);
            }
            return giveItemToHands(serverPlayer, hand, held);
        }
        InteractionResult tookFlower = CompanionFlowerGift.tryTakeOffer(this, serverPlayer, hand);
        if (tookFlower.consumesAction()) {
            return tookFlower;
        }
        return takeItemFromHands(serverPlayer, hand);
    }

    /**
     * Brief playful “evil mode”: temporary {@link CompanionAttitude#HOSTILE} toward nearby
     * non-owner targets, then auto-revert. Never attacks the owner. Ownership unchanged.
     */
    public void activatePlayfulEvil(int durationTicks) {
        if (level().isClientSide) {
            return;
        }
        int ticks = Math.max(5 * 20, Math.min(15 * 20, durationTicks));
        if (playfulEvilTicks <= 0) {
            playfulEvilRestoreAttitude = getAttitude();
        }
        playfulEvilDurationTicks = ticks;
        playfulEvilTicks = ticks;
        setAttitude(CompanionAttitude.HOSTILE);
        if (getMode() == FabricCompanionMode.SIT || getMode() == FabricCompanionMode.STAY) {
            setMode(FabricCompanionMode.FOLLOW);
        }
        sayOwnerChatLine("dialogue.azscompanions.evil_on");
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_ANGRY, SoundSource.NEUTRAL,
                0.35f, 1.4f + random.nextFloat() * 0.2f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SMOKE,
                    getX(), getY() + getBbHeight() * 0.8d, getZ(),
                    12, 0.35d, 0.25d, 0.35d, 0.02d);
            serverLevel.sendParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    getX(), getY() + getBbHeight() * 1.0d, getZ(),
                    4, 0.25d, 0.15d, 0.25d, 0.0d);
        }
    }

    public boolean isPlayfulEvil() {
        return playfulEvilTicks > 0;
    }

    private void tickPlayfulEvil() {
        if (playfulEvilTicks <= 0) {
            return;
        }
        playfulEvilTicks--;
        if (playfulEvilTicks > 0) {
            return;
        }
        setAttitude(playfulEvilRestoreAttitude == null
                ? CompanionAttitude.PASSIVE
                : playfulEvilRestoreAttitude);
        setTarget(null);
        playfulEvilDurationTicks = 0;
        sayOwnerChatLine("dialogue.azscompanions.evil_off");
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CAT_PURR, SoundSource.NEUTRAL,
                0.85f, 1.05f + random.nextFloat() * 0.1f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    getX(), getY() + getBbHeight() * 0.9d, getZ(),
                    6, 0.35d, 0.25d, 0.35d, 0.02d);
        }
    }

    private InteractionResult feedCakeSpawnChild(ServerPlayer player, InteractionHand hand) {
        FabricCompanionEntity child = FabricCompanionRecruitment.spawnChild(player, this);
        if (child == null) {
            player.displayClientMessage(Component.translatable("message.azscompanions.child_limit_reached"), true);
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            ItemStack stack = player.getItemInHand(hand);
            stack.shrink(1);
            player.setItemInHand(hand, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL,
                0.9f, 1.1f + random.nextFloat() * 0.15f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() * 0.9d, getZ(),
                    5, 0.35d, 0.2d, 0.35d, 0.02d);
        }
        player.displayClientMessage(Component.translatable(
                "message.azscompanions.child_spawned", child.getChatDisplayName()), true);
        return InteractionResult.CONSUME;
    }

    private InteractionResult feedPlayfulEvil(ServerPlayer player, InteractionHand hand) {
        if (!player.getAbilities().instabuild) {
            ItemStack stack = player.getItemInHand(hand);
            stack.shrink(1);
            player.setItemInHand(hand, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        activatePlayfulEvil(PLAYFUL_EVIL_DEFAULT_SECONDS * 20);
        swing(InteractionHand.MAIN_HAND, true);
        return InteractionResult.CONSUME;
    }

    private boolean isEdibleFood(ItemStack stack) {
        return !stack.isEmpty() && stack.get(DataComponents.FOOD) != null;
    }

    /** Eat 1 food from the player's hand (survival), cheer with hearts; never equip the food. */
    private InteractionResult feedFromPlayer(ServerPlayer player, InteractionHand hand) {
        // Survival: always consume exactly one. Creative: optional no-consume.
        if (!player.getAbilities().instabuild) {
            ItemStack stack = player.getItemInHand(hand);
            stack.shrink(1);
            player.setItemInHand(hand, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        heal(4.0f);
        swing(InteractionHand.MAIN_HAND, true);
        speakSuccess();
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CAT_PURR, SoundSource.NEUTRAL, 0.8f, 1.1f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    getX(),
                    getY() + getBbHeight() * 0.9d,
                    getZ(),
                    8,
                    0.4d, 0.3d, 0.4d,
                    0.02d);
        }
        return InteractionResult.CONSUME;
    }

    private InteractionResult giveItemToHands(ServerPlayer player, InteractionHand hand, ItemStack held) {
        if (FabricCompanionCharmItem.isCharm(held)) {
            return InteractionResult.PASS;
        }
        ItemStack main = inventory.getMainHand();
        ItemStack off = inventory.getOffHand();
        if (main.isEmpty()) {
            inventory.setItem(FabricCompanionInventory.MAIN_HAND, held.copy());
            player.setItemInHand(hand, ItemStack.EMPTY);
            player.displayClientMessage(Component.translatable("message.azscompanions.hand_given_main"), true);
            return InteractionResult.CONSUME;
        }
        if (off.isEmpty()) {
            inventory.setItem(FabricCompanionInventory.OFF_HAND, held.copy());
            player.setItemInHand(hand, ItemStack.EMPTY);
            player.displayClientMessage(Component.translatable("message.azscompanions.hand_given_off"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack previous = main.copy();
        inventory.setItem(FabricCompanionInventory.MAIN_HAND, held.copy());
        player.setItemInHand(hand, previous);
        player.displayClientMessage(Component.translatable("message.azscompanions.hand_swapped"), true);
        return InteractionResult.CONSUME;
    }

    private InteractionResult takeItemFromHands(ServerPlayer player, InteractionHand hand) {
        ItemStack main = inventory.getMainHand();
        if (!main.isEmpty()) {
            player.setItemInHand(hand, main.copy());
            inventory.setItem(FabricCompanionInventory.MAIN_HAND, ItemStack.EMPTY);
            player.displayClientMessage(Component.translatable("message.azscompanions.hand_taken_main"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack off = inventory.getOffHand();
        if (!off.isEmpty()) {
            player.setItemInHand(hand, off.copy());
            inventory.setItem(FabricCompanionInventory.OFF_HAND, ItemStack.EMPTY);
            player.displayClientMessage(Component.translatable("message.azscompanions.hand_taken_off"), true);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> inventory.getMainHand();
            case OFFHAND -> inventory.getOffHand();
            case HEAD -> inventory.getItem(FabricCompanionInventory.HEAD);
            case CHEST -> {
                ItemStack chest = inventory.getItem(FabricCompanionInventory.CHEST);
                // Wolf armor lives in the chest UI slot but renders via BODY on wolf proxies.
                yield CompanionArmorRules.isCanineArmor(chest) ? ItemStack.EMPTY : chest;
            }
            case LEGS -> inventory.getItem(FabricCompanionInventory.LEGS);
            case FEET -> inventory.getItem(FabricCompanionInventory.FEET);
            case BODY -> {
                ItemStack chest = inventory.getItem(FabricCompanionInventory.CHEST);
                yield CompanionArmorRules.isCanineArmor(chest) ? chest : super.getItemBySlot(slot);
            }
            default -> super.getItemBySlot(slot);
        };
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (FabricCompanionCharmItem.isCharm(stack)) {
            if (!level().isClientSide && !stack.isEmpty()) {
                this.spawnAtLocation(stack.copy());
            }
            return;
        }
        switch (slot) {
            case MAINHAND -> inventory.setItem(FabricCompanionInventory.MAIN_HAND, stack);
            case OFFHAND -> inventory.setItem(FabricCompanionInventory.OFF_HAND, stack);
            case HEAD -> inventory.setItem(FabricCompanionInventory.HEAD, stack);
            case CHEST -> inventory.setItem(FabricCompanionInventory.CHEST, stack);
            case LEGS -> inventory.setItem(FabricCompanionInventory.LEGS, stack);
            case FEET -> inventory.setItem(FabricCompanionInventory.FEET, stack);
            case BODY -> {
                if (stack.isEmpty() || CompanionArmorRules.isCanineArmor(stack)) {
                    inventory.setItem(FabricCompanionInventory.CHEST, stack);
                } else {
                    super.setItemSlot(slot, stack);
                }
            }
            default -> super.setItemSlot(slot, stack);
        }
    }

    public boolean canAttackTarget(LivingEntity target) {
        if (!FabricServerConfig.ALLOW_COMBAT) {
            return false;
        }
        if (!isAllowedCombatant(target)) {
            return false;
        }
        if (isTeamRival(target)) {
            return true;
        }
        if (getAttitude().isHostile()) {
            return true;
        }
        // Never pick random passives. Hostiles only if targeting us/owner or recent hurt link.
        if (!target.getType().getCategory().isFriendly()) {
            if (target instanceof Mob mob && (mob.getTarget() == this || mob.getTarget() == getOwner())) {
                return true;
            }
            return getLastHurtByMob() == target
                    || target.getLastHurtByMob() == this
                    || target.getLastHurtByMob() == getOwner();
        }
        return target.getLastHurtByMob() == this || target.getLastHurtByMob() == getOwner();
    }

    public void openInventory(ServerPlayer player) {
        player.openMenu(new FabricCompanionInventoryMenu.ExtendedProvider(this));
    }

    public void speakSuccess() {
        getDefinition().dialogue().pick(getDefinition().dialogue().success(), random)
                .ifPresent(line -> {
                    if (getOwner() instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.literal("<" + getChatDisplayName() + "> " + line), false);
                    }
                });
    }

    public void speakGreeting() {
        getDefinition().dialogue().pick(getDefinition().dialogue().greetings(), random)
                .ifPresent(line -> {
                    if (getOwner() instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.literal("<" + getChatDisplayName() + "> " + line), false);
                    }
                });
    }

    public String getChatDisplayName() {
        String override = entityData.get(DATA_NAME);
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        if (hasCustomName() && getCustomName() != null) {
            return getCustomName().getString();
        }
        return getDefinition().displayName();
    }

    /** Owner chat line for any form (player / animal / hostile). Used by CCI say + optional AI. */
    public void speakLine(String line) {
        if (level().isClientSide || line == null || line.isBlank()) {
            return;
        }
        String text = CompanionChatCensor.censorOutput(line.trim(), CompanionAiRuntime.get().settings());
        if (text == null || text.isBlank()) {
            return;
        }
        lastSpeakTick = tickCount;
        if (getOwner() instanceof ServerPlayer owner) {
            owner.displayClientMessage(Component.literal("<" + getChatDisplayName() + "> " + text), false);
        }
    }

    public void sayHello() {
        sayOwnerChatLine("dialogue.azscompanions.hello");
    }

    public void sayBye() {
        sayOwnerChatLine("dialogue.azscompanions.bye");
    }

    private void sayOwnerChatLine(String langKey) {
        if (level().isClientSide) {
            return;
        }
        if (!FabricServerConfig.COMPANION_CHAT_MESSAGES) {
            return;
        }
        if (getOwner() instanceof ServerPlayer owner) {
            String line = Component.translatable(langKey).getString();
            owner.displayClientMessage(Component.literal("<" + getChatDisplayName() + "> " + line), false);
        }
    }

    public FabricCompanionDefinition getDefinition() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(DATA_DEFINITION));
        return FabricCompanionRegistry.getOrKon(id == null ? FabricCompanionRegistry.KON_ID : id);
    }

    public void applyDefinition(FabricCompanionDefinition definition) {
        entityData.set(DATA_DEFINITION, definition.id().toString());
        setCustomDisplayName(definition.displayName());
        setSkinPath(definition.defaultSkin().toString());
        voiceProfile = definition.voiceProfile();
        setBodyScale(DEFAULT_BODY_SCALE);
        resetProportionsToDefaults();
    }

    public void setOwner(Player player) {
        setOwnerUuid(player.getUUID());
        setOwnerName(player.getGameProfile().getName());
        trusted.add(player.getUUID());
    }

    public void setOwnerUuid(UUID uuid) {
        entityData.set(DATA_OWNER, Optional.ofNullable(uuid));
        if (uuid != null) {
            applyOwnedNoDespawn();
        }
    }

    /**
     * Owned companions (and Bits) must not natural-despawn. Does not block intentional
     * discard (logout park, charm store, kill).
     */
    public void applyOwnedNoDespawn() {
        setPersistenceRequired();
        addTag(CompanionNoDespawnSupport.ENTITY_TAG);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return getOwnerUuid() == null && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    public String getOwnerName() {
        return ownerName == null ? "" : ownerName;
    }

    public void setOwnerName(String name) {
        this.ownerName = PlayerIdentityCompat.normalizeName(name);
    }

    public boolean isChildCompanion() {
        return leaderUuid != null;
    }

    public boolean isFightSpawn() {
        return fightSpawn || isChildCompanion();
    }

    public void setFightSpawn(boolean value) {
        this.fightSpawn = value;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID uuid) {
        this.leaderUuid = uuid;
        if (uuid != null) {
            this.fightSpawn = true;
        }
    }

    public int getStoredChildCount() {
        return entityData.get(DATA_STORED_CHILD_COUNT);
    }

    /** Effective Bit cap for this parent (living + stored). */
    public int getMaxChildren() {
        return CompanionChildLimits.clampMaxChildren(entityData.get(DATA_MAX_CHILDREN));
    }

    public void setMaxChildren(int max) {
        entityData.set(DATA_MAX_CHILDREN, CompanionChildLimits.clampMaxChildren(max));
    }

    private void syncStoredChildCount() {
        entityData.set(DATA_STORED_CHILD_COUNT, storedChildren.size());
    }

    public List<FabricCompanionEntity> listLivingChildren() {
        List<FabricCompanionEntity> out = new ArrayList<>();
        if (level().isClientSide || !(level() instanceof ServerLevel serverLevel)) {
            return out;
        }
        UUID self = getUUID();
        UUID owner = getOwnerUuid();
        for (FabricCompanionEntity child : serverLevel.getEntitiesOfClass(
                FabricCompanionEntity.class, getBoundingBox().inflate(256.0d),
                c -> c.isAlive() && self.equals(c.getLeaderUuid()))) {
            out.add(child);
        }
        if (serverLevel.getServer() != null) {
            for (ServerLevel other : serverLevel.getServer().getAllLevels()) {
                if (other == serverLevel) {
                    continue;
                }
                for (Entity entity : other.getAllEntities()) {
                    if (entity instanceof FabricCompanionEntity child
                            && child.isAlive()
                            && self.equals(child.getLeaderUuid())
                            && (owner == null || owner.equals(child.getOwnerUuid()))) {
                        out.add(child);
                    }
                }
            }
        }
        out.sort(Comparator.comparingInt((FabricCompanionEntity c) -> c.tickCount).reversed());
        return out;
    }

    public boolean storeChild(FabricCompanionEntity child) {
        if (level().isClientSide || child == null || !child.isAlive() || isChildCompanion()) {
            return false;
        }
        if (!getUUID().equals(child.getLeaderUuid())) {
            return false;
        }
        CompoundTag entry = new CompoundTag();
        entry.putUUID(CompanionStoredChildren.ENTRY_UUID, child.getUUID());
        CompoundTag data = new CompoundTag();
        child.saveWithoutId(data);
        entry.put(CompanionStoredChildren.ENTRY_DATA, data);
        storedChildren.add(entry);
        syncStoredChildCount();
        child.discard();
        return true;
    }

    public boolean storeNextLivingChild() {
        List<FabricCompanionEntity> living = listLivingChildren();
        if (living.isEmpty()) {
            return false;
        }
        return storeChild(living.getFirst());
    }

    public int storeAllLivingChildren() {
        int stored = 0;
        for (FabricCompanionEntity child : listLivingChildren()) {
            if (storeChild(child)) {
                stored++;
            }
        }
        return stored;
    }

    public FabricCompanionEntity callNextStoredChild(ServerPlayer player) {
        if (level().isClientSide || storedChildren.isEmpty() || isChildCompanion()) {
            return null;
        }
        if (FabricCompanionRecruitment.countChildrenOf(player, getUUID()) >= getMaxChildren()) {
            return null;
        }
        CompoundTag entry = storedChildren.getCompound(0);
        storedChildren.remove(0);
        syncStoredChildCount();
        UUID childUuid = entry.hasUUID(CompanionStoredChildren.ENTRY_UUID)
                ? entry.getUUID(CompanionStoredChildren.ENTRY_UUID)
                : UUID.randomUUID();
        CompoundTag data = entry.contains(CompanionStoredChildren.ENTRY_DATA)
                ? entry.getCompound(CompanionStoredChildren.ENTRY_DATA)
                : entry;
        FabricCompanionEntity child = FabricCompanionRecruitment.spawnStoredChild(
                player, this, data.copy(), childUuid);
        if (child == null) {
            storedChildren.add(0, entry);
            syncStoredChildCount();
        }
        return child;
    }

    public void despawnChildCompanions() {
        storeAllLivingChildren();
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide) {
            FabricCompanionChunkTickets.release(this);
        }
        super.remove(reason);
    }

    /** Allow following the owner through vanilla and modded dimensions. */
    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return true;
    }

    public boolean isOwnedBy(Player player) {
        if (player == null) {
            return false;
        }
        boolean nameFallback = IntegratedMultiplayerCompat.ownerNameFallbackEnabled(
                CompanionAiRuntime.get().settings());
        return PlayerIdentityCompat.isOwner(
                getOwnerUuid(),
                ownerName,
                player.getUUID(),
                player.getGameProfile().getName(),
                nameFallback);
    }

    public boolean isTrusted(Player player) {
        if (trusted.contains(player.getUUID())) {
            return true;
        }
        UUID owner = getOwnerUuid();
        return owner != null && com.azscompanions.compat.ftb.FtbCompat.isSameTeamAsOwner(owner, player.getUUID());
    }

    public Player getOwner() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        UUID owner = getOwnerUuid();
        if (owner != null) {
            ServerPlayer byUuid = serverLevel.getServer().getPlayerList().getPlayer(owner);
            if (byUuid != null) {
                return byUuid;
            }
        }
        if (!IntegratedMultiplayerCompat.ownerNameFallbackEnabled(CompanionAiRuntime.get().settings())) {
            return null;
        }
        String name = getOwnerName();
        if (name.isEmpty()) {
            return null;
        }
        for (ServerPlayer p : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (PlayerIdentityCompat.namesMatch(name, p.getGameProfile().getName())) {
                return p;
            }
        }
        return null;
    }

    public UUID getOwnerUuid() {
        return entityData.get(DATA_OWNER).orElse(null);
    }

    public FabricCompanionMode getMode() {
        return FabricCompanionMode.byName(entityData.get(DATA_MODE));
    }

    public void setMode(FabricCompanionMode mode) {
        entityData.set(DATA_MODE, mode.name());
        getNavigation().stop();
    }

    public FabricCompanionInventory getCompanionInventory() {
        return inventory;
    }

    public ItemStack getOfferedFlower() {
        return offeredFlower;
    }

    public void setOfferedFlower(ItemStack stack) {
        this.offeredFlower = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    public long getFlowerGiftCooldownUntil() {
        return flowerGiftCooldownUntil;
    }

    public void setFlowerGiftCooldownUntil(long gameTime) {
        this.flowerGiftCooldownUntil = gameTime;
    }

    public FabricTaskQueue getTaskQueue() {
        return taskQueue;
    }

    public BlockPos getHomePos() {
        return homePos;
    }

    public void setHomePos(BlockPos homePos) {
        this.homePos = homePos == null ? null : homePos.immutable();
    }

    public BlockPos getHomeBedPos() {
        return homeBedPos;
    }

    public void setHomeBedPos(BlockPos homeBedPos) {
        this.homeBedPos = homeBedPos == null ? null : homeBedPos.immutable();
    }

    public String getSkinPath() {
        return entityData.get(DATA_SKIN_PATH);
    }

    public void setSkinPath(String skinPath) {
        entityData.set(DATA_SKIN_PATH, skinPath == null ? "" : skinPath);
    }

    public String getSleepingSkinPath() {
        return entityData.get(DATA_SKIN_SLEEPING);
    }

    public void setSleepingSkinPath(String skinPath) {
        entityData.set(DATA_SKIN_SLEEPING, CompanionContextSkinSupport.sanitize(skinPath));
    }

    public String getBathingSkinPath() {
        return entityData.get(DATA_SKIN_BATHING);
    }

    public void setBathingSkinPath(String skinPath) {
        entityData.set(DATA_SKIN_BATHING, CompanionContextSkinSupport.sanitize(skinPath));
    }

    public String getAdventuringSkinPath() {
        return entityData.get(DATA_SKIN_ADVENTURING);
    }

    public void setAdventuringSkinPath(String skinPath) {
        entityData.set(DATA_SKIN_ADVENTURING, CompanionContextSkinSupport.sanitize(skinPath));
    }

    public void setContextSkins(String sleeping, String bathing, String adventuring) {
        setSleepingSkinPath(sleeping);
        setBathingSkinPath(bathing);
        setAdventuringSkinPath(adventuring);
    }

    public String getActiveContextSkinId() {
        return entityData.get(DATA_ACTIVE_CONTEXT);
    }

    public String getRenderSkinPath() {
        CompanionContextSkinSupport.Context active =
                CompanionContextSkinSupport.Context.byId(getActiveContextSkinId());
        return CompanionContextSkinSupport.resolveRenderSkinPath(
                getForm().isPlayer(),
                active,
                getSleepingSkinPath(),
                getBathingSkinPath(),
                getAdventuringSkinPath(),
                getSkinPath());
    }

    public boolean isSlimArms() {
        return entityData.get(DATA_SLIM);
    }

    public void setSlimArms(boolean slim) {
        entityData.set(DATA_SLIM, slim);
    }

    public CompanionGender getGender() {
        return CompanionGender.byName(entityData.get(DATA_GENDER));
    }

    public void setGender(CompanionGender gender) {
        CompanionGender value = gender == null ? CompanionGender.FEMALE : gender;
        entityData.set(DATA_GENDER, value.getSerializedName());
    }

    public CompanionForm getForm() {
        return CompanionForm.byName(entityData.get(DATA_FORM));
    }

    public void setForm(CompanionForm form) {
        CompanionForm value = form == null ? CompanionForm.PLAYER : form;
        CompanionForm previous = getForm();
        entityData.set(DATA_FORM, value.serializedName());
        if (previous != value) {
            setFormVariant(CompanionFormVariants.defaultVariant(value));
        }
        refreshDimensions();
        if (!level().isClientSide && previous != value) {
            ejectIncompatibleArmor();
        }
    }

    public String getFormVariant() {
        return CompanionFormVariants.normalize(getForm(), entityData.get(DATA_FORM_VARIANT));
    }

    public void setFormVariant(String variant) {
        entityData.set(DATA_FORM_VARIANT, CompanionFormVariants.normalize(getForm(), variant));
    }

    /** Move armor that this form cannot show into backpack (or drop) so slots stay honest. */
    public void ejectIncompatibleArmor() {
        if (level().isClientSide) {
            return;
        }
        CompanionForm form = getForm();
        int[] slots = {
                FabricCompanionInventory.HEAD,
                FabricCompanionInventory.CHEST,
                FabricCompanionInventory.LEGS,
                FabricCompanionInventory.FEET
        };
        EquipmentSlot[] uiSlots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = inventory.getItem(slots[i]);
            if (stack.isEmpty() || CompanionArmorRules.mayPlaceInArmorSlot(form, uiSlots[i], stack)) {
                continue;
            }
            inventory.setItem(slots[i], ItemStack.EMPTY);
            ItemStack leftover = inventory.insertItemAuto(stack);
            if (!leftover.isEmpty()) {
                this.spawnAtLocation(leftover);
            }
        }
    }

    /** Drop any Companion Charm that ended up in companion inventory/hands. */
    public void ejectForbiddenCharm() {
        if (level().isClientSide) {
            return;
        }
        for (int i = 0; i < FabricCompanionInventory.TOTAL_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!FabricCompanionCharmItem.isCharm(stack)) {
                continue;
            }
            inventory.setItem(i, ItemStack.EMPTY);
            this.spawnAtLocation(stack);
        }
    }

    /**
     * Form/scale are synched data — clients must refresh hitbox + name-tag attachment
     * when they arrive, or nametag height sticks to the previous form after a swap.
     */
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_FORM.equals(key)) {
            refreshDimensions();
        } else if (DATA_BODY_SCALE.equals(key)) {
            float scale = getBodyScale();
            var attr = getAttribute(Attributes.SCALE);
            if (attr != null && (float) attr.getBaseValue() != scale) {
                attr.setBaseValue(scale);
            }
            refreshDimensions();
        }
    }

    public boolean isNameTagVisible() {
        return entityData.get(DATA_SHOW_NAME_TAG);
    }

    public void setNameTagVisible(boolean visible) {
        entityData.set(DATA_SHOW_NAME_TAG, visible);
        setCustomNameVisible(visible);
    }

    /** When false, equipped armor still applies stats but is not rendered. */
    public boolean isArmorVisible() {
        return entityData.get(DATA_SHOW_ARMOR);
    }

    public void setArmorVisible(boolean visible) {
        entityData.set(DATA_SHOW_ARMOR, visible);
    }

    public CompanionAttitude getAttitude() {
        return CompanionAttitude.byName(entityData.get(DATA_ATTITUDE));
    }

    public void setAttitude(CompanionAttitude attitude) {
        CompanionAttitude value = attitude == null ? CompanionAttitude.PASSIVE : attitude;
        entityData.set(DATA_ATTITUDE, value.serializedName());
        if (value == CompanionAttitude.PASSIVE && getTarget() != null && !isTeamRival(getTarget())) {
            setTarget(null);
        }
    }

    public String getTeamId() {
        return entityData.get(DATA_TEAM);
    }

    public void setTeamId(String teamId) {
        String sanitized = teamId == null ? "" : teamId.trim();
        if (sanitized.length() > 32) {
            sanitized = sanitized.substring(0, 32);
        }
        entityData.set(DATA_TEAM, sanitized);
    }

    public float getFollowRadius() {
        return entityData.get(DATA_FOLLOW_RADIUS);
    }

    public void setFollowRadius(float radius) {
        float follow = CompanionFollowDistances.clampFollowRadius(radius);
        entityData.set(DATA_FOLLOW_RADIUS, follow);
        // Wander must stay ≥ follow (bump when follow is raised past wander).
        if (getWanderRadius() < follow) {
            entityData.set(DATA_WANDER_RADIUS, CompanionFollowDistances.clampWanderRadius(follow));
        }
    }

    public float getPersonalSpace() {
        return entityData.get(DATA_PERSONAL_SPACE);
    }

    public void setPersonalSpace(float space) {
        entityData.set(DATA_PERSONAL_SPACE, CompanionFollowDistances.clampPersonalSpace(space));
    }

    public float getWanderRadius() {
        return entityData.get(DATA_WANDER_RADIUS);
    }

    public void setWanderRadius(float radius) {
        entityData.set(DATA_WANDER_RADIUS,
                CompanionFollowDistances.clampWanderRadius(radius, getFollowRadius()));
    }

    public com.azscompanions.ai.CompanionPersona getPersona() {
        return persona == null ? com.azscompanions.ai.CompanionPersona.EMPTY : persona;
    }

    public void setPersona(com.azscompanions.ai.CompanionPersona next) {
        this.persona = next == null ? com.azscompanions.ai.CompanionPersona.EMPTY : next;
    }

    public boolean isPersonaInitialized() {
        return getPersona().initialized();
    }

    /** When false, this companion skips entity chunk tickets (server global must still be on). */
    public boolean isChunkLoadingEnabled() {
        return chunkLoadingEnabled;
    }

    public void setChunkLoadingEnabled(boolean enabled) {
        this.chunkLoadingEnabled = enabled;
        if (!enabled && !level().isClientSide && level() instanceof ServerLevel) {
            FabricCompanionChunkTickets.release(this);
        }
    }


    /** Copy spacing from a parent; Bits get a slightly tighter leash. */
    public void inheritSpacingFrom(FabricCompanionEntity parent) {
        if (parent == null) {
            return;
        }
        float childFollow = CompanionFollowDistances.inheritFollowRadius(parent.getFollowRadius());
        setFollowRadius(childFollow);
        setPersonalSpace(CompanionFollowDistances.inheritPersonalSpace(parent.getPersonalSpace()));
        setWanderRadius(CompanionFollowDistances.inheritWanderRadius(parent.getWanderRadius(), childFollow));
    }

    public boolean wantsAggressiveTargets() {
        return getAttitude().isHostile() || (getTeamId() != null && !getTeamId().isBlank());
    }

    public boolean isValidHostilePrey(LivingEntity target) {
        if (!isAllowedCombatant(target)) {
            return false;
        }
        if (isTeamRival(target)) {
            return true;
        }
        return getAttitude().isHostile();
    }

    private boolean isAllowedCombatant(LivingEntity target) {
        if (target == null || !target.isAlive() || target == this) {
            return false;
        }
        if (target instanceof Player player && (isOwnedBy(player) || isTrusted(player))) {
            return false;
        }
        if (target instanceof FabricCompanionEntity other) {
            if (CompanionTeamColors.sameTeam(getTeamId(), other.getTeamId())) {
                return false;
            }
        }
        if (target instanceof OwnableEntity ownable) {
            UUID petOwner = ownable.getOwnerUUID();
            if (petOwner != null && (petOwner.equals(getOwnerUuid()) || trusted.contains(petOwner))) {
                return target instanceof FabricCompanionEntity && isTeamRival(target);
            }
        }
        return true;
    }

    private boolean isTeamRival(LivingEntity target) {
        if (!(target instanceof FabricCompanionEntity other)) {
            return false;
        }
        String mine = getTeamId();
        String theirs = other.getTeamId();
        if (mine == null || mine.isBlank() || theirs == null || theirs.isBlank()) {
            return false;
        }
        return !CompanionTeamColors.sameTeam(mine, theirs);
    }

    public boolean isMale() {
        return getGender().isMale();
    }

    public float getBodyScale() {
        return entityData.get(DATA_BODY_SCALE);
    }

    public void setBodyScale(float scale) {
        float clamped = Math.max(MIN_BODY_SCALE, Math.min(MAX_BODY_SCALE, scale));
        entityData.set(DATA_BODY_SCALE, clamped);
        var attr = getAttribute(Attributes.SCALE);
        if (attr != null) {
            attr.setBaseValue(clamped);
        }
        refreshDimensions();
    }

    public void setCustomDisplayName(String name) {
        boolean wasKon = isKonNamed();
        String trimmed = name == null ? "" : name.trim();
        entityData.set(DATA_NAME, trimmed);
        if (!trimmed.isEmpty()) {
            setCustomName(Component.literal(trimmed));
        }
        if (isKonNamed() && !wasKon) {
            applyKonSpecialDefaults();
        }
    }

    public boolean isKonNamed() {
        String override = entityData.get(DATA_NAME);
        return override != null && !override.isBlank() && override.trim().equalsIgnoreCase("Kon");
    }

    public void applyOwnerAppearanceDefaults(ServerPlayer player) {
        if (com.azscompanions.AzsCompanionsConstants.isPeckerOwner(player.getUUID())) {
            setForm(CompanionForm.CHICKEN);
            setCustomDisplayName(com.azscompanions.AzsCompanionsConstants.PECKER_COMPANION_NAME);
            setSkinPath("");
            setNameTagVisible(true);
            return;
        }
        if (com.azscompanions.AzsCompanionsConstants.isWolfyOwner(player.getUUID())) {
            setForm(CompanionForm.WOLF);
            setFormVariant(WolfyPerkSupport.BROWN_WOLF_VARIANT_ID);
            setCustomDisplayName(com.azscompanions.AzsCompanionsConstants.WOLFY_COMPANION_NAME);
            setSkinPath("");
            setNameTagVisible(true);
            return;
        }
        setCustomDisplayName(player.getGameProfile().getName());
        if (!isKonNamed()) {
            setSkinPath("player:" + player.getUUID());
        }
    }

    public void applyKonSpecialDefaults() {
        FabricCompanionDefinition def = FabricCompanionRegistry.getOrKon(FabricCompanionRegistry.KON_ID);
        setSkinPath(def.defaultSkin().toString());
        grantKonBedToOwnerOnce();
    }

    private void grantKonBedToOwnerOnce() {
        if (konBedGranted || level().isClientSide) {
            return;
        }
        if (!(getOwner() instanceof ServerPlayer player)) {
            return;
        }
        konBedGranted = true;
        ItemStack bed = new ItemStack(FabricModItems.KON_BED);
        if (!player.getInventory().add(bed)) {
            player.drop(bed, false);
        }
        player.displayClientMessage(Component.translatable("message.azscompanions.kon_bed_granted"), true);
    }

    public float getBust() {
        return entityData.get(DATA_BUST);
    }

    public void setBust(float v) {
        entityData.set(DATA_BUST, CompanionBodyProportions.clampBust(v));
    }

    public float getWaist() {
        return entityData.get(DATA_WAIST);
    }

    public void setWaist(float v) {
        entityData.set(DATA_WAIST, CompanionBodyProportions.clampWaist(v));
    }

    public float getHips() {
        return entityData.get(DATA_HIPS);
    }

    public void setHips(float v) {
        entityData.set(DATA_HIPS, CompanionBodyProportions.clampHips(v));
    }

    public float getShoulders() {
        return entityData.get(DATA_SHOULDERS);
    }

    public void setShoulders(float v) {
        entityData.set(DATA_SHOULDERS, CompanionBodyProportions.clampShoulders(v));
    }

    public float getBustOffset() {
        return entityData.get(DATA_BUST_OFFSET);
    }

    public void setBustOffset(float v) {
        entityData.set(DATA_BUST_OFFSET, CompanionBodyProportions.clampBustOffset(v));
    }

    public void resetProportionsToDefaults() {
        setBust(CompanionBodyProportions.DEFAULT_BUST);
        setWaist(CompanionBodyProportions.DEFAULT_WAIST);
        setHips(CompanionBodyProportions.DEFAULT_HIPS);
        setShoulders(CompanionBodyProportions.DEFAULT_SHOULDERS);
        setBustOffset(CompanionBodyProportions.DEFAULT_BUST_OFFSET);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = getOwnerUuid();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        if (ownerName != null && !ownerName.isBlank()) {
            tag.putString("OwnerName", ownerName);
        }
        if (leaderUuid != null) {
            tag.putUUID("LeaderUuid", leaderUuid);
        }
        tag.putBoolean("FightSpawn", fightSpawn);
        tag.putString("Definition", entityData.get(DATA_DEFINITION));
        tag.putString("Mode", entityData.get(DATA_MODE));
        tag.putString("SkinPath", getSkinPath());
        tag.putString("SkinSleeping", getSleepingSkinPath());
        tag.putString("SkinBathing", getBathingSkinPath());
        tag.putString("SkinAdventuring", getAdventuringSkinPath());
        tag.putString("CustomNameOverride", entityData.get(DATA_NAME));
        tag.putString("CompanionForm", getForm().serializedName());
        tag.putString(CompanionFormVariants.NBT_KEY, getFormVariant());
        tag.putBoolean("ShowNameTag", isNameTagVisible());
        tag.putBoolean("ShowArmor", isArmorVisible());
        tag.putString("Attitude", getAttitude().serializedName());
        tag.putString("TeamId", getTeamId() == null ? "" : getTeamId());
        tag.putFloat("FollowRadius", getFollowRadius());
        tag.putFloat("PersonalSpace", getPersonalSpace());
        tag.putFloat("WanderRadius", getWanderRadius());
        tag.putBoolean("SlimArms", isSlimArms());
        tag.putString("Gender", getGender().getSerializedName());
        tag.putBoolean("KonBedGranted", konBedGranted);
        tag.putFloat("BodyScale", getBodyScale());
        tag.putFloat("Bust", getBust());
        tag.putFloat("Waist", getWaist());
        tag.putFloat("Hips", getHips());
        tag.putFloat("Shoulders", getShoulders());
        tag.putFloat("BustOffset", getBustOffset());
        tag.putString("VoiceProfile", voiceProfile);
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_WHO, persona.whoAmI());
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_WHAT, persona.whatAmIDoing());
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_HOW, persona.howWillIBe());
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_SPEECH, persona.speechStyle());
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_RELATIONSHIP, persona.relationshipToOwner());
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_QUIRKS, persona.quirks());
        tag.putBoolean(com.azscompanions.ai.CompanionPersona.NBT_INITIALIZED, persona.initialized());
        tag.putBoolean("ChunkLoading", chunkLoadingEnabled);
        if (homePos != null) {
            tag.putLong("HomePos", homePos.asLong());
        }
        if (homeBedPos != null) {
            tag.putLong("HomeBedPos", homeBedPos.asLong());
        }
        tag.put("Inventory", inventory.createTag(level().registryAccess()));
        tag.put(CompanionStoredChildren.NBT_LIST, storedChildren.copy());
        tag.putInt("MaxChildren", getMaxChildren());
        if (!offeredFlower.isEmpty()) {
            tag.put("OfferedFlower", offeredFlower.save(level().registryAccess(), new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getBaseValue() != 20.0d) {
            maxHealth.setBaseValue(20.0d);
            if (getHealth() > 20.0f) {
                setHealth(20.0f);
            }
        }
        if (tag.hasUUID("Owner")) {
            setOwnerUuid(tag.getUUID("Owner"));
        }
        if (tag.contains("OwnerName")) {
            setOwnerName(tag.getString("OwnerName"));
        }
        if (tag.hasUUID("LeaderUuid")) {
            leaderUuid = tag.getUUID("LeaderUuid");
        } else {
            leaderUuid = null;
        }
        fightSpawn = tag.contains("FightSpawn") && tag.getBoolean("FightSpawn");
        if (tag.contains("Definition")) {
            entityData.set(DATA_DEFINITION, tag.getString("Definition"));
        }
        if (tag.contains("Mode")) {
            entityData.set(DATA_MODE, tag.getString("Mode"));
        }
        if (tag.contains("SkinPath")) {
            setSkinPath(tag.getString("SkinPath"));
        }
        if (tag.contains("SkinSleeping")) {
            setSleepingSkinPath(tag.getString("SkinSleeping"));
        }
        if (tag.contains("SkinBathing")) {
            setBathingSkinPath(tag.getString("SkinBathing"));
        }
        if (tag.contains("SkinAdventuring")) {
            setAdventuringSkinPath(tag.getString("SkinAdventuring"));
        }
        konBedGranted = tag.contains("KonBedGranted") && tag.getBoolean("KonBedGranted");
        if (tag.contains("CustomNameOverride") && !tag.getString("CustomNameOverride").isEmpty()) {
            // Load name without re-triggering Kon special grants.
            String override = tag.getString("CustomNameOverride");
            entityData.set(DATA_NAME, override);
            setCustomName(Component.literal(override));
        }
        if (tag.contains("CompanionForm")) {
            setForm(CompanionForm.byName(tag.getString("CompanionForm")));
        } else {
            setForm(CompanionForm.PLAYER);
        }
        if (tag.contains(CompanionFormVariants.NBT_KEY)) {
            setFormVariant(tag.getString(CompanionFormVariants.NBT_KEY));
        } else if (getForm() == CompanionForm.WOLF
                && WolfyPerkSupport.isWolfyName(getChatDisplayName())) {
            setFormVariant(WolfyPerkSupport.BROWN_WOLF_VARIANT_ID);
        } else {
            setFormVariant(CompanionFormVariants.defaultVariant(getForm()));
        }
        if (tag.contains("ShowNameTag")) {
            setNameTagVisible(tag.getBoolean("ShowNameTag"));
        } else {
            setNameTagVisible(true);
        }
        if (tag.contains("ShowArmor")) {
            setArmorVisible(tag.getBoolean("ShowArmor"));
        } else {
            setArmorVisible(true);
        }
        if (tag.contains("Attitude")) {
            setAttitude(CompanionAttitude.byName(tag.getString("Attitude")));
        } else {
            setAttitude(CompanionAttitude.PASSIVE);
        }
        if (tag.contains("TeamId")) {
            setTeamId(tag.getString("TeamId"));
        } else {
            setTeamId("");
        }
        setFollowRadius(tag.contains("FollowRadius")
                ? tag.getFloat("FollowRadius")
                : CompanionFollowDistances.DEFAULT_FOLLOW_RADIUS);
        setPersonalSpace(tag.contains("PersonalSpace")
                ? tag.getFloat("PersonalSpace")
                : CompanionFollowDistances.DEFAULT_PERSONAL_SPACE);
        setWanderRadius(tag.contains("WanderRadius")
                ? tag.getFloat("WanderRadius")
                : CompanionFollowDistances.DEFAULT_WANDER_RADIUS);
        if (tag.contains("SlimArms")) {
            setSlimArms(tag.getBoolean("SlimArms"));
        }
        if (tag.contains("Gender")) {
            setGender(CompanionGender.byName(tag.getString("Gender")));
        } else {
            setGender(CompanionGender.FEMALE);
        }
        setBodyScale(tag.contains("BodyScale") ? tag.getFloat("BodyScale") : DEFAULT_BODY_SCALE);
        setBust(tag.contains("Bust") ? tag.getFloat("Bust") : CompanionBodyProportions.DEFAULT_BUST);
        setWaist(tag.contains("Waist") ? tag.getFloat("Waist") : CompanionBodyProportions.DEFAULT_WAIST);
        setHips(tag.contains("Hips") ? tag.getFloat("Hips") : CompanionBodyProportions.DEFAULT_HIPS);
        setShoulders(tag.contains("Shoulders") ? tag.getFloat("Shoulders") : CompanionBodyProportions.DEFAULT_SHOULDERS);
        setBustOffset(tag.contains("BustOffset") ? tag.getFloat("BustOffset") : CompanionBodyProportions.DEFAULT_BUST_OFFSET);
        if (tag.contains("VoiceProfile")) {
            voiceProfile = tag.getString("VoiceProfile");
        }
        persona = new com.azscompanions.ai.CompanionPersona(
                tag.contains(com.azscompanions.ai.CompanionPersona.NBT_WHO)
                        ? tag.getString(com.azscompanions.ai.CompanionPersona.NBT_WHO) : "",
                tag.contains(com.azscompanions.ai.CompanionPersona.NBT_WHAT)
                        ? tag.getString(com.azscompanions.ai.CompanionPersona.NBT_WHAT) : "",
                tag.contains(com.azscompanions.ai.CompanionPersona.NBT_HOW)
                        ? tag.getString(com.azscompanions.ai.CompanionPersona.NBT_HOW) : "",
                tag.contains(com.azscompanions.ai.CompanionPersona.NBT_SPEECH)
                        ? tag.getString(com.azscompanions.ai.CompanionPersona.NBT_SPEECH) : "",
                tag.contains(com.azscompanions.ai.CompanionPersona.NBT_RELATIONSHIP)
                        ? tag.getString(com.azscompanions.ai.CompanionPersona.NBT_RELATIONSHIP) : "",
                tag.contains(com.azscompanions.ai.CompanionPersona.NBT_QUIRKS)
                        ? tag.getString(com.azscompanions.ai.CompanionPersona.NBT_QUIRKS) : "",
                tag.contains(com.azscompanions.ai.CompanionPersona.NBT_INITIALIZED)
                        && tag.getBoolean(com.azscompanions.ai.CompanionPersona.NBT_INITIALIZED)
        );
        if (tag.contains("ChunkLoading")) {
            chunkLoadingEnabled = tag.getBoolean("ChunkLoading");
        } else {
            chunkLoadingEnabled = true;
        }
        if (tag.contains("HomePos")) {
            homePos = BlockPos.of(tag.getLong("HomePos"));
        }
        if (tag.contains("HomeBedPos")) {
            homeBedPos = BlockPos.of(tag.getLong("HomeBedPos"));
        } else if (homePos != null) {
            homeBedPos = homePos;
        }
        if (tag.contains("Inventory")) {
            inventory.fromTag(tag.getList("Inventory", CompoundTag.TAG_COMPOUND), level().registryAccess());
        }
        storedChildren.clear();
        if (tag.contains(CompanionStoredChildren.NBT_LIST, Tag.TAG_LIST)) {
            ListTag list = tag.getList(CompanionStoredChildren.NBT_LIST, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                storedChildren.add(list.getCompound(i).copy());
            }
        }
        syncStoredChildCount();
        if (tag.contains("MaxChildren")) {
            setMaxChildren(tag.getInt("MaxChildren"));
        } else {
            setMaxChildren(FabricServerConfig.MAX_CHILD_COMPANIONS_PER_LEADER);
        }
        if (tag.contains("OfferedFlower", Tag.TAG_COMPOUND)) {
            offeredFlower = ItemStack.parse(level().registryAccess(), tag.getCompound("OfferedFlower"))
                    .orElse(ItemStack.EMPTY);
        } else {
            offeredFlower = ItemStack.EMPTY;
        }
        ejectIncompatibleArmor();
        ejectForbiddenCharm();
    }
}
