package com.azscompanions.entity;

import com.azscompanions.AzsCompanions;
import com.azscompanions.ai.ChildAutonomyMode;
import com.azscompanions.ai.CompanionAiActionTrust;
import com.azscompanions.ai.CompanionAiAsk;
import com.azscompanions.ai.CompanionAiChatSupport;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.ai.CompanionChatCensor;
import com.azscompanions.ai.CompanionRecentAction;
import com.azscompanions.ai.CompanionChatEventSupport;
import com.azscompanions.ai.CompanionRecentActionMemory;
import com.azscompanions.config.CommonConfig;
import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionPlayMode;
import com.azscompanions.entity.ai.CompanionFollowGoal;
import com.azscompanions.entity.ai.CompanionHostileTargetGoal;
import com.azscompanions.entity.ai.CompanionLookAtOwnerGoal;
import com.azscompanions.entity.ai.CompanionOwnerDefendTargetGoal;
import com.azscompanions.entity.ai.CompanionPotionBehaviorGoal;
import com.azscompanions.entity.ai.CompanionRideAlongGoal;
import com.azscompanions.entity.ai.CompanionSitGoal;
import com.azscompanions.entity.ai.CompanionSleepInBedGoal;
import com.azscompanions.entity.ai.CompanionWanderMobInteractGoal;
import com.azscompanions.entity.ai.CompanionWanderNearOwnerGoal;
import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.menu.CompanionInventoryMenu;
import com.azscompanions.menu.CompanionManagementMenu;
import com.azscompanions.network.packet.OpenCompanionMenuPacket;
import com.azscompanions.perk.MisterWigglySidekick;
import com.azscompanions.perk.SpecialPlayerPerks;
import com.azscompanions.perk.WolfyPerkSupport;
import com.azscompanions.item.CompanionCharmItem;
import com.azscompanions.registry.ModItems;
import com.azscompanions.task.TaskQueue;
import com.azscompanions.util.CompanionArmorRules;
import com.azscompanions.util.CompanionPotionHelper;
import com.azscompanions.util.NbtUuids;
import com.azscompanions.util.ProtectionHelper;
import com.azscompanions.voice.DialogueCategory;
import com.azscompanions.voice.VoiceService;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adult companion NPC. Never damages its owner, trusted players, pets, or protected targets.
 */
public class CompanionEntity extends PathfinderMob {
    /** Body scale clamp: 0.5 (tiny adult) … 3.0 (large). Default 0.7. Maps 1:1 to {@link Attributes#SCALE}. */
    public static final float MIN_BODY_SCALE = 0.5f;
    public static final float MAX_BODY_SCALE = 3.0f;
    public static final float DEFAULT_BODY_SCALE = 0.7f;

    private static final EntityDataAccessor<String> DATA_DEFINITION =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_MODE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_CUSTOM_NAME_OVERRIDE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SITTING =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_BODY_SCALE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_SKIN_PATH =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN_SLEEPING =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN_BATHING =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SKIN_ADVENTURING =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ACTIVE_CONTEXT =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SLIM_ARMS =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_GENDER =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_BUST =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WAIST =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HIPS =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHOULDERS =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_BUST_OFFSET =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_FORM =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_FORM_VARIANT =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SHOW_NAME_TAG =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SHOW_ARMOR =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_ATTITUDE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TEAM =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_FOLLOW_RADIUS =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PERSONAL_SPACE =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WANDER_RADIUS =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.FLOAT);
    /** Synced so client UI ownership checks work without looking at NBT. */
    private static final EntityDataAccessor<String> DATA_OWNER =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
    /** Callable Bits parked on this parent (FIFO store → call). Synced for menu icon. */
    private static final EntityDataAccessor<Integer> DATA_STORED_CHILD_COUNT =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);
    /** Per-companion Bit cap (default 3; CCI maxChildren= override). */
    private static final EntityDataAccessor<Integer> DATA_MAX_CHILDREN =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.INT);

    private final CompanionInventory inventory = new CompanionInventory();
    /** FIFO snapshots of Bits removed from the world (menu Remove / charm parent store). */
    private final ListTag storedChildren = new ListTag();
    private final TaskQueue taskQueue = new TaskQueue(this);
    private final OwnerActivityTracker ownerActivity = new OwnerActivityTracker();
    private final Set<UUID> trustedPlayers = new HashSet<>();
    private final Set<String> permissions = new HashSet<>();
    @Nullable
    private BlockPos homePos;
    @Nullable
    private BlockPos homeBedPos;
    @Nullable
    private BlockPos guardCenter;
    private int guardRadius = 8;
    private int stuckTicks;
    private Vec3 lastPos = Vec3.ZERO;
    private String voiceProfile = "kon_soft";
    /** Last-known owner profile name for hosted-world UUID remapping. */
    private String ownerName = "";
    private String pronouns = "she/her";
    private String behaviorStyle = "gentle";
    private boolean chunkLoadingEnabled = true;
    /** Once Kon-identity bed grant has been given to the owner. */
    private boolean konBedGranted;
    /** Transient playful “turn evil” countdown (ticks). Not persisted. */
    private int playfulEvilTicks;
    private int playfulEvilDurationTicks;
    private CompanionAttitude playfulEvilRestoreAttitude = CompanionAttitude.PASSIVE;
    /** CCI/cake child Bit — UUID of leader companion. */
    @Nullable
    private UUID leaderUuid;
    /** Team-fight or Bit spawn; excluded from maxCompanionsPerPlayer. */
    private boolean fightSpawn;
    /** CCI/stream temporary summon — expires and is never the player's charm companion. */
    private boolean cciSummoned;
    private long cciExpireAtGameTime;
    private float cciMaxHealth;
    private boolean applyingPlayerPersistentData;

    void beginApplyingPlayerPersistentData() {
        applyingPlayerPersistentData = true;
    }

    void endApplyingPlayerPersistentData() {
        applyingPlayerPersistentData = false;
    }
    private int nextIdleChatTick;
    private int ownerAwayTicks;
    private int lastCallPlayerTick = Integer.MIN_VALUE / 4;
    private int lastSpeakTick;
    private CompanionPlayMode playMode = CompanionPlayMode.NONE;
    private int playTicksRemaining;
    private BlockPos playHideTarget;
    /** Per-companion AI persona (who / what / how). Persisted in NBT; charm store preserves it. */
    private com.azscompanions.ai.CompanionPersona persona = com.azscompanions.ai.CompanionPersona.EMPTY;
    /** Pending flower the companion offers after a gift; empty when none. Persisted. */
    private ItemStack offeredFlower = ItemStack.EMPTY;
    /** Game time when the next flower gift is allowed. Not persisted. */
    private long flowerGiftCooldownUntil;
    /** Mounted via owner ride-along; sync-dismount when the owner dismounts. Not persisted. */
    private boolean rideAlongActive;
    // skinPath / bodyScale / slimArms live in synched entity data

    /** Default playful-evil duration when no CCI {@code seconds=} is given. */
    public static final int PLAYFUL_EVIL_DEFAULT_SECONDS = 10;

    public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanFloat(true);
        }
        // Allow pathing through water when following a swimming owner (default WATER malus is high).
        this.setPathfindingMalus(PathType.WATER, 0.0f);
        this.setPathfindingMalus(PathType.WATER_BORDER, 0.0f);
        this.setCanPickUpLoot(true);
        inventory.setPersistenceHook(() -> CompanionPlayerDataSupport.save(this));
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        // Vanilla loot vacuum must not scoop harmful/neutral potions; AI goal only targets beneficial.
        if (CompanionPotionHelper.isPotionItem(stack)) {
            return CompanionPotionHelper.isAutoPickupAllowed(stack);
        }
        return super.wantsToPickUp(level, stack);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0d)
                .add(Attributes.MOVEMENT_SPEED, 0.32d)
                .add(Attributes.ATTACK_DAMAGE, CompanionCombatDamage.NETHERITE_SWORD_ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 64.0d)
                .add(Attributes.ARMOR, 2.0d)
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
        builder.define(DATA_DEFINITION, CompanionRegistry.KON_ID.toString());
        builder.define(DATA_MODE, CompanionMode.FOLLOW.getSerializedName());
        builder.define(DATA_CUSTOM_NAME_OVERRIDE, "");
        builder.define(DATA_SITTING, false);
        builder.define(DATA_BODY_SCALE, DEFAULT_BODY_SCALE);
        builder.define(DATA_SKIN_PATH, "");
        builder.define(DATA_SKIN_SLEEPING, "");
        builder.define(DATA_SKIN_BATHING, "");
        builder.define(DATA_SKIN_ADVENTURING, "");
        builder.define(DATA_ACTIVE_CONTEXT, "");
        builder.define(DATA_SLIM_ARMS, false);
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
        builder.define(DATA_OWNER, "");
        builder.define(DATA_STORED_CHILD_COUNT, 0);
        builder.define(DATA_MAX_CHILDREN, CompanionChildLimits.MAX_PER_LEADER);
    }

    @Override
    protected void registerGoals() {
        registerNormalGoals();
    }

    private void registerNormalGoals() {
        // Sit/stay stop movement; combat/potions/sleep outrank follow; wander when owner idle nearby.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CompanionSitGoal(this));
        goalSelector.addGoal(2, new CompanionSleepInBedGoal(this));
        goalSelector.addGoal(3, new CompanionPotionBehaviorGoal(this));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.25d, true));
        goalSelector.addGoal(5, new CompanionRideAlongGoal(this));
        goalSelector.addGoal(6, new CompanionFollowGoal(this));
        goalSelector.addGoal(7, new CompanionWanderMobInteractGoal(this));
        goalSelector.addGoal(8, new CompanionWanderNearOwnerGoal(this));
        goalSelector.addGoal(9, new OpenDoorGoal(this, true));
        goalSelector.addGoal(10, new CompanionLookAtOwnerGoal(this));
        goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(12, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new CompanionOwnerDefendTargetGoal(this));
        targetSelector.addGoal(2, new CompanionHostileTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }


    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && level() instanceof ServerLevel serverLevel) {
            if (getOwnerUuid() != null
                    && (!isPersistenceRequired()
                            || !entityTags().contains(CompanionNoDespawnSupport.ENTITY_TAG))) {
                applyOwnedNoDespawn();
            }
            {
                CompanionMode mode = getMode();
                if (mode != CompanionMode.FOLLOW
                        && mode != CompanionMode.SIT
                        && mode != CompanionMode.STAY
                        && mode != CompanionMode.WANDER
                        && mode != CompanionMode.TASK) {
                    setMode(CompanionMode.FOLLOW);
                }
            }
            taskQueue.tick(serverLevel);
            SpecialPlayerPerks.applyCompanionPerks(this, getOwnerUuid());
            tickOwnerActivity();
            tickContextSkinState();
            tickHomeBedLeash();
            tickSleepPurr();
            tickPlayfulEvil();
            tickAiAmbientSpeech();
            tickStripLuck();
            tickPlayBehavior();
            tickChildParentLeash();
            tickRideAlongSync();
            if (tickCount % 40 == 0) {
                MisterWigglySidekick.ensureFor(this);
            }
            tickStuckRecovery();
            tickSurvival();
            if (tickCount % 20 == 0) {
                ejectForbiddenCharm();
            }
            tickCciSummonExpiry(serverLevel);
            maintainInvincibility();
        }
    }

    /**
     * Home-bed rescue teleport: only when the owner is farther than
     * {@link ServerConfig#HOME_BED_RADIUS} from the home bed, and only if the companion is also
     * farther than {@link CompanionFollowDistances#MIN_TELEPORT_DISTANCE} (never short-range snaps).
     * Stay/Sit ignore this. Wander uses this as its only teleport-to-owner path.
     */
    private void tickHomeBedLeash() {
        CompanionMode mode = getMode();
        if (mode == CompanionMode.STAY || mode == CompanionMode.SIT) {
            return;
        }
        if (mode != CompanionMode.FOLLOW && mode != CompanionMode.WANDER) {
            return;
        }
        if (getTarget() != null && getTarget().isAlive()) {
            return;
        }
        if (isSleeping()) {
            return;
        }
        Player owner = getOwner();
        BlockPos bed = getHomeBedPos();
        if (owner == null || bed == null) {
            return;
        }
        double radius = ServerConfig.HOME_BED_RADIUS.get();
        if (owner.distanceToSqr(bed.getX() + 0.5d, bed.getY(), bed.getZ() + 0.5d) <= radius * radius) {
            return;
        }
        double dist = distanceTo(owner);
        // Never snap when already near the owner — walk/follow handles closing the gap.
        if (CompanionFollowDistances.tooCloseToTeleport(dist, getFollowRadius())) {
            return;
        }
        safeTeleportNear(owner.blockPosition());
    }

    /** Soft cat purr every few seconds while asleep — Kon-named companions only. */
    private void tickSleepPurr() {
        if (!isSleeping() || !isKonNamed()) {
            return;
        }
        // ~5s cadence, staggered by entity id so multiple companions don't sync.
        if ((tickCount + getId()) % 100 != 0) {
            return;
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CAT_PURR_BABY.value(), SoundSource.NEUTRAL, 0.55f, 0.95f + random.nextFloat() * 0.15f);
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
        boolean bathing = CompanionContextSkinSupport.isBathing(isSleeping(), isInWater());
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
     */

    @Override
    public boolean addEffect(net.minecraft.world.effect.MobEffectInstance effect, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.Entity source) {
        if (!com.azscompanions.entity.CompanionLuckSupport.luckAffectsCompanion()
                && effect != null
                && isLuckMobEffect(effect.getEffect())) {
            return false;
        }
        return super.addEffect(effect, source);
    }

    private void tickStripLuck() {
        if (com.azscompanions.entity.CompanionLuckSupport.luckAffectsCompanion() || tickCount % 20 != 0) {
            return;
        }
        stripLuckEffectsAndModifiers();
    }

    private void stripLuckEffectsAndModifiers() {
        removeEffect(net.minecraft.world.effect.MobEffects.LUCK);
        removeEffect(net.minecraft.world.effect.MobEffects.UNLUCK);
        var luck = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.LUCK);
        if (luck != null) {
            luck.removeModifiers();
            luck.setBaseValue(0.0d);
        }
    }

    private static boolean isLuckMobEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        if (effect == null) {
            return false;
        }
        if (effect.is(net.minecraft.world.effect.MobEffects.LUCK) || effect.is(net.minecraft.world.effect.MobEffects.UNLUCK)) {
            return true;
        }
        var key = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        return key != null && com.azscompanions.entity.CompanionLuckSupport.isLuckEffectId(key.toString());
    }

    private void tickAiAmbientSpeech() {
        if (tickCount % 20 != 0) {
            return;
        }
        CompanionAiRuntime runtime = CompanionAiRuntime.get();
        CompanionAiSettings settings = runtime.settings();
        if (!settings.idleChat() && !settings.reactiveChat() && !settings.callPlayerWhenAway()) {
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
        java.util.function.Predicate<com.azscompanions.ai.CompanionRecentAction> allowReactive =
                a -> com.azscompanions.ai.CompanionChatEventSupport.allowReactiveAction(settings, a);
        boolean hasReactive = settings.reactiveChat()
                && CompanionRecentActionMemory.hasReactive(owner.getUUID(), gameTime, allowReactive);
        int speakCoolSec = hasReactive
                ? CompanionAiChatSupport.reactiveSpeakCooldownSeconds()
                : CompanionAiChatSupport.idleSpeakCooldownSeconds();
        if (lastSpeakTick > 0 && CompanionAiChatSupport.spokeTooRecently(tickCount - lastSpeakTick, speakCoolSec)) {
            return;
        }
        String ownerName = owner.getGameProfile().name();
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
                    if (!CompanionAiAsk.askQuiet(owner, this, ownerName, prompt,
                            CompanionAiActionTrust.OWNER, null, fallback)) {
                        speakLine(fallback);
                    }
                } else {
                    speakLine(fallback);
                }
                if (getMode() == CompanionMode.FOLLOW) {
                    getNavigation().moveTo(owner, 1.15d);
                }
                return;
            }
        } else {
            ownerAwayTicks = 0;
        }

        if ((!settings.idleChat() && !hasReactive) || (llmOn && runtime.isBusy())) {
            return;
        }
        boolean child = getLeaderUuid() != null;
        double idleMul = child ? settings.childAutonomy().idleIntervalMultiplier() : 1.0d;
        if (dist > settings.chatReactRange()) {
            return;
        }
        boolean reactiveNow = hasReactive;
        if (!reactiveNow) {
            if (nextIdleChatTick <= 0) {
                nextIdleChatTick = tickCount + CompanionAiChatSupport.nextIdleDelayTicks(
                        settings.idleChatSecondsMin(), settings.idleChatSecondsMax(), idleMul, random::nextInt);
                return;
            }
            if (tickCount < nextIdleChatTick) {
                return;
            }
            if (CompanionAiChatSupport.shouldSkipIdleRoll(random::nextInt)) {
                nextIdleChatTick = tickCount + CompanionAiChatSupport.nextIdleDelayTicks(
                        settings.idleChatSecondsMin(), settings.idleChatSecondsMax(), idleMul, random::nextInt);
                return;
            }
        }
        if (CompanionAiChatSupport.playerAmbientTooRecent(owner.getUUID(), gameTime, reactiveNow)) {
            if (!reactiveNow) {
                nextIdleChatTick = tickCount + CompanionAiChatSupport.nextIdleDelayTicks(
                        settings.idleChatSecondsMin(), settings.idleChatSecondsMax(), idleMul, random::nextInt);
            }
            return;
        }
        CompanionRecentAction focus = hasReactive
                ? CompanionRecentActionMemory.consumeReactive(owner.getUUID(), gameTime, allowReactive).orElse(null)
                : null;
        reactiveNow = focus != null;
        if (hasReactive && focus == null) {
            return;
        }
        if (!reactiveNow && !settings.idleChat()) {
            return;
        }
        nextIdleChatTick = tickCount + CompanionAiChatSupport.nextIdleDelayTicks(
                settings.idleChatSecondsMin(), settings.idleChatSecondsMax(), idleMul, random::nextInt);
        var recent = CompanionRecentActionMemory.peek(owner.getUUID(), gameTime);
        String fallback = focus != null
                ? CompanionAiChatSupport.fallbackReactiveLine(ownerName, focus)
                : CompanionAiChatSupport.fallbackIdleLine(ownerName, owner.getUUID());
        if (!llmOn) {
            speakLine(fallback);
            return;
        }
        String prompt;
        if (child && settings.childAutonomy().prefersTalkToParent() && level() instanceof ServerLevel sl
                && sl.getEntity(getLeaderUuid()) instanceof CompanionEntity parent) {
            prompt = "[ambient child] Talk briefly to your parent " + parent.getChatDisplayName()
                    + " — one short wholesome line.";
        } else {
            prompt = CompanionAiChatSupport.ambientPromptWithRecent(ownerName, focus, recent);
        }
        if (!CompanionAiAsk.askQuiet(owner, this, ownerName, prompt,
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
                    int ox = getBlockX() + random.nextInt(11) - 5;
                    int oz = getBlockZ() + random.nextInt(11) - 5;
                    playHideTarget = new BlockPos(ox, getBlockY(), oz);
                }
                if (blockPosition().distManhattan(playHideTarget) > 2) {
                    getNavigation().moveTo(playHideTarget.getX() + 0.5, playHideTarget.getY(), playHideTarget.getZ() + 0.5, 1.15d);
                } else {
                    setMode(CompanionMode.SIT);
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
                    setMode(CompanionMode.SIT);
                    getNavigation().stop();
                } else {
                    setMode(CompanionMode.FOLLOW);
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

    /** Soft leash: child Bits path back toward parent leader when beyond autonomy radius. */
    private void tickChildParentLeash() {
        UUID leaderId = getLeaderUuid();
        if (leaderId == null || tickCount % 20 != 0) {
            return;
        }
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (!(level.getEntity(leaderId) instanceof CompanionEntity parent) || !parent.isAlive()) {
            return;
        }
        CompanionAiSettings settings = CompanionAiRuntime.get().settings();
        double leash = settings.effectiveChildLeashRadius();
        double dist = distanceTo(parent);
        if (dist <= leash) {
            if (settings.childAutonomy().allowsCuriousWander()
                    && settings.childAutonomy() == ChildAutonomyMode.CURIOUS
                    && random.nextInt(40) == 0
                    && getMode() == CompanionMode.FOLLOW) {
                // Short curious poke near parent
                double a = random.nextDouble() * Math.PI * 2;
                getNavigation().moveTo(getX() + Math.cos(a) * 2.5, getY(), getZ() + Math.sin(a) * 2.5, 1.05d);
            }
            return;
        }
        setMode(CompanionMode.FOLLOW);
        getNavigation().moveTo(parent, 1.2d);
        if (dist > leash * 2.5d && !CompanionFollowDistances.tooCloseToTeleport(dist, getFollowRadius())) {
            safeTeleportNear(parent.blockPosition());
        }
    }

    /** Shared idle/explore tracker for follow, wander, and teleport gates. */
    public OwnerActivityTracker getOwnerActivity() {
        return ownerActivity;
    }

    /** True when the owner has been mostly still (~2.5s) — wander, no teleport. */
    public boolean isOwnerStandingAround() {
        return ownerActivity.isStandingAround();
    }

    /** True when the owner is moving meaningfully — normal follow + long-range teleport. */
    public boolean isOwnerExploring() {
        return ownerActivity.isExploring();
    }

    /** Configured home-bed radius (NeoForge server config, default 35). */
    public double getHomeBedRadius() {
        return ServerConfig.HOME_BED_RADIUS.get();
    }

    /** True when a home bed is set and this companion is within the home-bed radius. */
    public boolean isNearHomeBed() {
        BlockPos bed = getHomeBedPos();
        if (bed == null) {
            return false;
        }
        double r = getHomeBedRadius();
        return distanceToSqr(bed.getX() + 0.5d, bed.getY(), bed.getZ() + 0.5d) <= r * r;
    }

    /** True when owner is farther than home-bed radius from the home bed. */
    public boolean isOwnerFarFromHomeBed() {
        Player owner = getOwner();
        BlockPos bed = getHomeBedPos();
        if (owner == null || bed == null) {
            return false;
        }
        double r = getHomeBedRadius();
        return owner.distanceToSqr(bed.getX() + 0.5d, bed.getY(), bed.getZ() + 0.5d) > r * r;
    }

    /**
     * Actively trail the owner (vs home-idle / Wander stroll).
     * Stay/Sit never. Wander without a far-from-bed rescue never glue-follows (stroll only).
     * No home bed → Follow only (Wander strolls near owner, no follow leash).
     */
    public boolean shouldActivelyFollowOwner() {
        CompanionMode mode = getMode();
        if (mode == CompanionMode.STAY || mode == CompanionMode.SIT) {
            return false;
        }
        if (mode == CompanionMode.WANDER) {
            // Only after the home-bed rescue condition (owner left bed radius).
            return getHomeBedPos() != null && isOwnerFarFromHomeBed();
        }
        if (mode != CompanionMode.FOLLOW) {
            return false;
        }
        if (getHomeBedPos() == null) {
            return true;
        }
        if (isOwnerFarFromHomeBed()) {
            return true;
        }
        // Owner still near home: stay home-idle while companion is near the bed.
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

    /** Home-idle / Wander near bed when bed exists and owner has not left the home radius. */
    public boolean shouldHomeIdleNearBed() {
        CompanionMode mode = getMode();
        if (mode == CompanionMode.STAY || mode == CompanionMode.SIT) {
            return false;
        }
        if (getHomeBedPos() == null || isOwnerFarFromHomeBed()) {
            return false;
        }
        return isNearHomeBed() && (mode == CompanionMode.FOLLOW || mode == CompanionMode.WANDER);
    }

    @Override
    public void remove(RemovalReason reason) {
        if (isFullyInvincible() && reason == RemovalReason.KILLED) {
            setHealth(getMaxHealth());
            return;
        }
        if (!level().isClientSide()) {
            CompanionPlayerDataSupport.save(this);
            MisterWigglySidekick.despawnFor(this);
        }
        super.remove(reason);
    }

    @Override
    protected void onBelowWorld() {
        Player owner = getOwner();
        if (owner != null) {
            safeTeleportNear(owner.blockPosition());
            setHealth(getMaxHealth());
            return;
        }
        if (isFullyInvincible()) {
            setHealth(getMaxHealth());
            return;
        }
        super.onBelowWorld();
    }

    @Override
    public boolean fireImmune() {
        return isFullyInvincible() || super.fireImmune();
    }

    @Override
    public void die(DamageSource source) {
        if (isFullyInvincible()) {
            setHealth(getMaxHealth());
            return;
        }
        super.die(source);
    }

    private void tickStuckRecovery() {
        if (position().distanceToSqr(lastPos) < 0.01d && !getNavigation().isDone()) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPos = position();
        }
        if (stuckTicks > CommonConfig.PATH_STUCK_TIMEOUT_TICKS.get() && CommonConfig.TELEPORT_WHEN_STUCK.get()) {
            // Never stuck-teleport during Wander / home-idle / fight / standing-around.
            CompanionMode mode = getMode();
            if (mode == CompanionMode.WANDER || mode == CompanionMode.STAY || mode == CompanionMode.SIT
                    || shouldHomeIdleNearBed()) {
                stuckTicks = 0;
                return;
            }
            if (getTarget() != null && getTarget().isAlive()) {
                stuckTicks = 0;
                return;
            }
            if (isOwnerStandingAround()) {
                stuckTicks = 0;
                return;
            }
            Player owner = getOwner();
            if (owner != null) {
                double dist = distanceTo(owner);
                if (!CompanionFollowDistances.tooCloseToTeleport(dist, getFollowRadius())
                        && CompanionFollowDistances.shouldGroundTeleport(dist, getFollowRadius())
                        && isOwnerExploring()) {
                    safeTeleportNear(owner.blockPosition());
                    stuckTicks = 0;
                    speak(DialogueCategory.TASK_PROGRESS);
                }
            }
        }
    }

    private void tickSurvival() {
        if (getHealth() / getMaxHealth() <= ServerConfig.LOW_HEALTH_RETREAT_RATIO.get()) {
            if (tickCount % CompanionAiChatSupport.LOW_HEALTH_SPEAK_INTERVAL_TICKS == 0) {
                speak(DialogueCategory.LOW_HEALTH);
            }
            if (CommonConfig.ENABLE_HEALING_SYSTEM.get()) {
                tryEatFood();
            }
        }
        if (inventory.isFull() && tickCount % CompanionAiChatSupport.INVENTORY_FULL_SPEAK_INTERVAL_TICKS == 0) {
            speak(DialogueCategory.INVENTORY_FULL);
        }
    }

    private void tryEatFood() {
        ItemStack food = inventory.getFoodSlot();
        if (!food.isEmpty() && food.has(net.minecraft.core.component.DataComponents.FOOD)) {
            heal(4.0f);
            food.shrink(1);
        }
    }

    public void safeTeleportNear(BlockPos target) {
        if (isPassenger()) {
            return;
        }
        // Stay/Sit hold position — never teleport (like sitting cats/dogs).
        CompanionMode mode = getMode();
        if (mode == CompanionMode.STAY || mode == CompanionMode.SIT || isSitting()) {
            return;
        }
        double preferred = CompanionFollowDistances.preferredDistance(getPersonalSpace());
        for (int[] off : CompanionSafeTeleportSupport.horizontalOffsets(preferred)) {
            for (int dy : CompanionSafeTeleportSupport.Y_OFFSETS) {
                BlockPos candidate = target.offset(off[0], dy, off[1]);
                if (isSafeTeleportStand(candidate)) {
                    finishSafeTeleport(candidate.getX() + 0.5d, candidate.getY(), candidate.getZ() + 0.5d);
                    return;
                }
            }
        }
        Player owner = getOwner();
        float yaw = owner != null ? owner.getYRot() : getYRot();
        double[] behind = CompanionSafeTeleportSupport.behindOwner(yaw, Math.max(2.5d, preferred));
        BlockPos fallback = target.offset(
                (int) Math.round(behind[0]), 0, (int) Math.round(behind[1]));
        for (int dy : CompanionSafeTeleportSupport.Y_OFFSETS) {
            BlockPos candidate = fallback.offset(0, dy, 0);
            if (isSafeTeleportStand(candidate)) {
                finishSafeTeleport(candidate.getX() + 0.5d, candidate.getY(), candidate.getZ() + 0.5d);
                return;
            }
        }
        finishSafeTeleport(fallback.getX() + 0.5d, fallback.getY(), fallback.getZ() + 0.5d);
    }

    private boolean isSafeTeleportStand(BlockPos pos) {
        var feet = level().getBlockState(pos);
        var head = level().getBlockState(pos.above());
        var below = level().getBlockState(pos.below());
        if (!below.isSolid()) {
            return false;
        }
        if (!feet.getCollisionShape(level(), pos).isEmpty()) {
            return false;
        }
        if (!head.getCollisionShape(level(), pos.above()).isEmpty()) {
            return false;
        }
        if (!feet.getFluidState().isEmpty() && feet.getFluidState().is(FluidTags.LAVA)) {
            return false;
        }
        Player owner = getOwner();
        return owner == null || !pos.equals(owner.blockPosition());
    }

    private void finishSafeTeleport(double x, double y, double z) {
        teleportTo(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        fallDistance = 0.0f;
        invulnerableTime = Math.max(invulnerableTime, CompanionSafeTeleportSupport.POST_TELEPORT_INVULN_TICKS);
        if (isFullyInvincible()) {
            setInvulnerable(true);
            setHealth(getMaxHealth());
        }
    }

    public boolean isFullyInvincible() {
        return CompanionInvincibilitySupport.isFullyInvincible(
                isKonNamed(),
                isChildCompanion(),
                getChatDisplayName(),
                entityData.get(DATA_DEFINITION),
                isCciSummoned());
    }

    @Override
    public void setHealth(float health) {
        if (isFullyInvincible()
                && CompanionInvincibilitySupport.shouldRejectHealthDrop(true, getHealth(), health)) {
            super.setHealth(getMaxHealth());
            return;
        }
        super.setHealth(health);
    }

    private void maintainInvincibility() {
        if (!isFullyInvincible()) {
            return;
        }
        setInvulnerable(true);
        if (getHealth() < getMaxHealth()) {
            setHealth(getMaxHealth());
        }
        if (isOnFire()) {
            clearFire();
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!isOwnedBy(player) && !isTrusted(player)) {
            player.sendOverlayMessage(Component.translatable("message.azscompanions.not_owner"));
            return InteractionResult.CONSUME;
        }

        // Hold charm + Shift + right-click opens shared menu (Customize | Command | Inventory).
        if (player.isShiftKeyDown()) {
            ItemStack heldForMenu = player.getItemInHand(hand);
            if (CompanionCharmItem.isCharm(heldForMenu)) {
                PacketDistributor.sendToPlayer(serverPlayer, new OpenCompanionMenuPacket(getId()));
                return InteractionResult.CONSUME;
            }
            // Shift without charm in this hand: do not open menu or swap items.
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        // Charm or empty hand on parent: call next stored Bit (callable count decreases).
        if (!isChildCompanion() && getStoredChildCount() > 0
                && (held.isEmpty() || CompanionCharmItem.isCharm(held))) {
            CompanionEntity called = callNextStoredChild(serverPlayer);
            if (called != null) {
                player.sendOverlayMessage(Component.translatable(
                        "message.azscompanions.child_called", called.getChatDisplayName()));
                return InteractionResult.CONSUME;
            }
            if (getStoredChildCount() > 0) {
                player.sendOverlayMessage(Component.translatable("message.azscompanions.child_limit_reached"));
                return InteractionResult.CONSUME;
            }
        }
        if (!held.isEmpty()) {
            if (CompanionCharmItem.isCharm(held)) {
                // Do not put the charm into companion hands.
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
        if (level().isClientSide()) {
            return;
        }
        int ticks = Math.max(5 * 20, Math.min(15 * 20, durationTicks));
        if (playfulEvilTicks <= 0) {
            playfulEvilRestoreAttitude = getAttitude();
        }
        playfulEvilDurationTicks = ticks;
        playfulEvilTicks = ticks;
        setAttitude(CompanionAttitude.HOSTILE);
        CompanionMode mode = getMode();
        if (mode == CompanionMode.SIT || mode == CompanionMode.STAY) {
            setMode(CompanionMode.FOLLOW);
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
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CAT_PURR_BABY.value(), SoundSource.NEUTRAL,
                0.85f, 1.05f + random.nextFloat() * 0.1f);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    getX(), getY() + getBbHeight() * 0.9d, getZ(),
                    6, 0.35d, 0.25d, 0.35d, 0.02d);
        }
    }

    private InteractionResult feedCakeSpawnChild(ServerPlayer player, InteractionHand hand) {
        CompanionEntity child = CompanionRecruitment.spawnChild(player, this);
        if (child == null) {
            player.sendOverlayMessage(Component.translatable("message.azscompanions.child_limit_reached"));
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
        player.sendOverlayMessage(Component.translatable(
                "message.azscompanions.child_spawned", child.getChatDisplayName()));
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
        return !stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.FOOD);
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
        speak(DialogueCategory.SUCCESS);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CAT_PURR_BABY.value(), SoundSource.NEUTRAL, 0.8f, 1.1f);
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

    /**
     * Give held item: fill main hand first, then offhand; if both full, swap into main hand.
     */
    private InteractionResult giveItemToHands(ServerPlayer player, InteractionHand hand, ItemStack held) {
        if (CompanionCharmItem.isCharm(held)) {
            return InteractionResult.PASS;
        }
        ItemStack main = inventory.getMainHand();
        ItemStack off = inventory.getOffHand();
        if (main.isEmpty()) {
            inventory.setStackInSlot(CompanionInventory.MAIN_HAND, held.copy());
            player.setItemInHand(hand, ItemStack.EMPTY);
            player.sendOverlayMessage(Component.translatable("message.azscompanions.hand_given_main"));
            return InteractionResult.CONSUME;
        }
        if (off.isEmpty()) {
            inventory.setStackInSlot(CompanionInventory.OFF_HAND, held.copy());
            player.setItemInHand(hand, ItemStack.EMPTY);
            player.sendOverlayMessage(Component.translatable("message.azscompanions.hand_given_off"));
            return InteractionResult.CONSUME;
        }
        // Both occupied — swap with main hand and return old main to player.
        ItemStack previous = main.copy();
        inventory.setStackInSlot(CompanionInventory.MAIN_HAND, held.copy());
        player.setItemInHand(hand, previous);
        player.sendOverlayMessage(Component.translatable("message.azscompanions.hand_swapped"));
        return InteractionResult.CONSUME;
    }

    /** Empty-hand take: main hand first, then offhand. */
    private InteractionResult takeItemFromHands(ServerPlayer player, InteractionHand hand) {
        ItemStack main = inventory.getMainHand();
        if (!main.isEmpty()) {
            player.setItemInHand(hand, main.copy());
            inventory.setStackInSlot(CompanionInventory.MAIN_HAND, ItemStack.EMPTY);
            player.sendOverlayMessage(Component.translatable("message.azscompanions.hand_taken_main"));
            return InteractionResult.CONSUME;
        }
        ItemStack off = inventory.getOffHand();
        if (!off.isEmpty()) {
            player.setItemInHand(hand, off.copy());
            inventory.setStackInSlot(CompanionInventory.OFF_HAND, ItemStack.EMPTY);
            player.sendOverlayMessage(Component.translatable("message.azscompanions.hand_taken_off"));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /** Hand + armor slots are backed by companion inventory so items render and persist with charm NBT. */
    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> inventory.getMainHand();
            case OFFHAND -> inventory.getOffHand();
            case HEAD -> inventory.getStackInSlot(CompanionInventory.HEAD);
            case CHEST -> {
                ItemStack chest = inventory.getStackInSlot(CompanionInventory.CHEST);
                // Wolf armor lives in the chest UI slot but renders via BODY on wolf proxies.
                yield CompanionArmorRules.isCanineArmor(chest) ? ItemStack.EMPTY : chest;
            }
            case LEGS -> inventory.getStackInSlot(CompanionInventory.LEGS);
            case FEET -> inventory.getStackInSlot(CompanionInventory.FEET);
            case BODY -> {
                ItemStack chest = inventory.getStackInSlot(CompanionInventory.CHEST);
                yield CompanionArmorRules.isCanineArmor(chest) ? chest : super.getItemBySlot(slot);
            }
            default -> super.getItemBySlot(slot);
        };
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (CompanionCharmItem.isCharm(stack)) {
            if (!level().isClientSide() && !stack.isEmpty() && level() instanceof ServerLevel serverLevel) {
                this.spawnAtLocation(serverLevel, stack.copy());
            }
            return;
        }
        switch (slot) {
            case MAINHAND -> inventory.setStackInSlot(CompanionInventory.MAIN_HAND, stack);
            case OFFHAND -> inventory.setStackInSlot(CompanionInventory.OFF_HAND, stack);
            case HEAD -> inventory.setStackInSlot(CompanionInventory.HEAD, stack);
            case CHEST -> inventory.setStackInSlot(CompanionInventory.CHEST, stack);
            case LEGS -> inventory.setStackInSlot(CompanionInventory.LEGS, stack);
            case FEET -> inventory.setStackInSlot(CompanionInventory.FEET, stack);
            case BODY -> {
                if (stack.isEmpty() || CompanionArmorRules.isCanineArmor(stack)) {
                    inventory.setStackInSlot(CompanionInventory.CHEST, stack);
                } else {
                    super.setItemSlot(slot, stack);
                }
            }
            default -> super.setItemSlot(slot, stack);
        }
    }

    public void openManagement(ServerPlayer player) {
        player.openMenu(new CompanionManagementMenu.Provider(this), buf -> buf.writeVarInt(getId()));
    }

    public void openInventory(ServerPlayer player) {
        player.openMenu(new CompanionInventoryMenu.Provider(this), buf -> buf.writeVarInt(getId()));
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (isFullyInvincible()) {
            return true;
        }
        if (CompanionHazardImmunity.ignores(source.typeHolder().unwrapKey()
                .map(key -> key.identifier().getPath())
                .orElse(""))) {
            return true;
        }
        return super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isFullyInvincible()) {
            setHealth(getMaxHealth());
            return false;
        }
        if (source.getEntity() instanceof Player player && (isOwnedBy(player) || isTrusted(player))) {
            return false;
        }
        if (isInvulnerableTo(level, source)) {
            return false;
        }
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) {
            speak(DialogueCategory.DANGER);
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
        if (!(target instanceof LivingEntity living)) {
            return false;
        }
        if (!canAttackTarget(living)) {
            return false;
        }
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            return super.doHurtTarget(level, target);
        }
        return CompanionCombatDamage.withFixedMeleeDamage(
                attack.getValue(),
                attack.getBaseValue(),
                attack::setBaseValue,
                () -> super.doHurtTarget(level, target));
    }

    public boolean canAttackTarget(LivingEntity target) {
        if (!ServerConfig.ALLOW_COMBAT.get() || !hasPermission("combat")) {
            return false;
        }
        if (!isAllowedCombatant(target)) {
            return false;
        }
        if (ProtectionHelper.isProtectedEntity(target)) {
            return false;
        }
        if (isTeamRival(target)) {
            return true;
        }
        if (getAttitude().isHostile()) {
            return true;
        }
        if (ServerConfig.ATTACK_NEUTRALS_ONLY_IF_HIT.get() && !target.getType().getCategory().isFriendly()) {
            return true;
        }
        return target.getLastHurtByMob() == this || target.getLastHurtByMob() == getOwner();
    }

    public boolean canBreakBlock(BlockPos pos) {
        if (!ServerConfig.ALLOW_GRIEFING.get() && ProtectionHelper.isProtectedBlock(level(), pos, getOwner())) {
            return false;
        }
        return ProtectionHelper.canCompanionModify(level(), pos, this);
    }

    
    @Override
    protected void dropEquipment(ServerLevel level) {
        if (CompanionInventoryPersistence.shouldKeepInventoryOnDeath(ServerConfig.KEEP_INVENTORY_ON_DEATH.get())) {
            return;
        }
        super.dropEquipment(level);
    }

    public void speak(DialogueCategory category) {
        if (lastSpeakTick > 0 && CompanionAiChatSupport.spokeTooRecently(
                tickCount - lastSpeakTick, CompanionAiChatSupport.scriptedSpeakCooldownSeconds())) {
            return;
        }
        CompanionDefinition definition = getDefinition();
        definition.dialogue().pick(category.lines(definition.dialogue()), random)
                .ifPresent(line -> {
                    lastSpeakTick = tickCount;
                    if (getOwner() instanceof ServerPlayer owner) {
                        long gameTime = level() instanceof ServerLevel sl ? sl.getGameTime() : tickCount;
                        CompanionAiChatSupport.recordAmbientSpeak(owner.getUUID(), gameTime, line);
                    }
                    VoiceService.get().speak(this, category, line);
                });
    }

    /** Display name used in chat tags (custom rename, else definition name). */
    public String getChatDisplayName() {
        String override = entityData.get(DATA_CUSTOM_NAME_OVERRIDE);
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
        if (level().isClientSide() || line == null || line.isBlank()) {
            return;
        }
        String text = CompanionChatCensor.censorOutput(line.trim(), CompanionAiRuntime.get().settings());
        if (text == null || text.isBlank()) {
            return;
        }
        text = CompanionAiChatSupport.shortenSpokenLine(text);
        if (text.isBlank()) {
            return;
        }
        if (getOwner() instanceof ServerPlayer owner) {
            if (CompanionAiChatSupport.isSameAsLastLine(owner.getUUID(), text)) {
                return;
            }
            lastSpeakTick = tickCount;
            long gameTime = level() instanceof ServerLevel sl ? sl.getGameTime() : tickCount;
            CompanionAiChatSupport.recordAmbientSpeak(owner.getUUID(), gameTime, text);
            // Chat only — scripted VoiceService uses the action-bar overlay, not both.
            owner.sendSystemMessage(Component.literal("<" + getChatDisplayName() + "> " + text));
        }
    }

    /** Owner chat line when the companion appears via charm. */
    public void sayHello() {
        sayOwnerChatLine("dialogue.azscompanions.hello");
    }

    /** Owner chat line when the companion is stored via charm. */
    public void sayBye() {
        sayOwnerChatLine("dialogue.azscompanions.bye");
    }

    private void sayOwnerChatLine(String langKey) {
        if (level().isClientSide()) {
            return;
        }
        if (!ServerConfig.COMPANION_CHAT_MESSAGES.get()) {
            return;
        }
        if (getOwner() instanceof ServerPlayer owner) {
            String line = Component.translatable(langKey).getString();
            owner.sendSystemMessage(Component.literal("<" + getChatDisplayName() + "> " + line));
        }
    }

    public CompanionDefinition getDefinition() {
        Identifier id = Identifier.tryParse(entityData.get(DATA_DEFINITION));
        return CompanionRegistry.getOrKon(id == null ? CompanionRegistry.KON_ID : id);
    }

    public void applyDefinition(CompanionDefinition definition) {
        entityData.set(DATA_DEFINITION, definition.id().toString());
        if (entityData.get(DATA_CUSTOM_NAME_OVERRIDE).isEmpty()) {
            setCustomName(Component.literal(definition.displayName()));
        }
        voiceProfile = definition.voiceProfile();
        behaviorStyle = definition.behaviorStyle();
        if (!definition.pronouns().isEmpty()) {
            pronouns = String.join("/", definition.pronouns());
        }
        permissions.clear();
        permissions.addAll(definition.defaultPermissions());
        setSkinPath(definition.defaultSkin().toString());
        setBodyScale(DEFAULT_BODY_SCALE);
        resetProportionsToDefaults();
    }

    public boolean isOwnedBy(Player player) {
        UUID owner = getOwnerUuid();
        return owner != null && owner.equals(player.getUUID());
    }

    public boolean isTrusted(Player player) {
        if (trustedPlayers.contains(player.getUUID())) {
            return true;
        }
        if (!com.azscompanions.config.ServerConfig.ALLOW_TEAM_TRUST.get()) {
            return false;
        }
        UUID owner = getOwnerUuid();
        return owner != null && com.azscompanions.compat.ftb.FtbCompat.isSameTeamAsOwner(owner, player.getUUID());
    }

    public void setOwner(Player player) {
        setOwnerUuid(player.getUUID());
        setOwnerName(player.getGameProfile().name());
        trustedPlayers.add(player.getUUID());
    }

    public void setOwnerUuid(@Nullable UUID uuid) {
        entityData.set(DATA_OWNER, uuid == null ? "" : uuid.toString());
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

    public boolean isChildCompanion() {
        return leaderUuid != null;
    }

    public boolean isFightSpawn() {
        return fightSpawn || isChildCompanion();
    }

    public void setFightSpawn(boolean value) {
        this.fightSpawn = value;
    }

    public boolean isCciSummoned() {
        return cciSummoned;
    }

    public void markCciSummoned(long expireAtGameTime) {
        this.cciSummoned = true;
        this.fightSpawn = true;
        this.cciExpireAtGameTime = expireAtGameTime;
        setInvulnerable(false);
    }

    public void setCciMaxHealth(float health) {
        this.cciMaxHealth = health;
    }

    private void tickCciSummonExpiry(ServerLevel serverLevel) {
        if (!CompanionCciSummonSupport.shouldExpire(cciSummoned, cciExpireAtGameTime, serverLevel.getGameTime())) {
            return;
        }
        setInvulnerable(false);
        hurt(damageSources().genericKill(), Float.MAX_VALUE);
    }

    @Nullable
    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(@Nullable UUID uuid) {
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

    /** Living world Bits + stored callable snapshots under this parent. */
    public int getOccupiedChildSlots(ServerPlayer player) {
        int living = player == null ? 0 : CompanionRecruitment.countChildrenOf(player, getUUID());
        return living + getStoredChildCount();
    }

    private void syncStoredChildCount() {
        entityData.set(DATA_STORED_CHILD_COUNT, storedChildren.size());
    }

    private CompoundTag snapshotEntity(CompanionEntity entity) {
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(entity.problemPath(), AzsCompanions.LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
            entity.saveWithoutId(output);
            return output.buildResult();
        }
    }

    /** Living Bits under this parent, oldest first (approx. creation order). */
    public List<CompanionEntity> listLivingChildren() {
        List<CompanionEntity> out = new ArrayList<>();
        if (level().isClientSide() || !(level() instanceof ServerLevel serverLevel)) {
            return out;
        }
        UUID self = getUUID();
        UUID owner = getOwnerUuid();
        for (CompanionEntity child : serverLevel.getEntitiesOfClass(
                CompanionEntity.class, getBoundingBox().inflate(256.0d),
                c -> c.isAlive() && self.equals(c.getLeaderUuid()))) {
            out.add(child);
        }
        if (serverLevel.getServer() != null) {
            for (ServerLevel other : serverLevel.getServer().getAllLevels()) {
                if (other == serverLevel) {
                    continue;
                }
                for (Entity entity : other.getAllEntities()) {
                    if (entity instanceof CompanionEntity child
                            && child.isAlive()
                            && self.equals(child.getLeaderUuid())
                            && (owner == null || owner.equals(child.getOwnerUuid()))) {
                        out.add(child);
                    }
                }
            }
        }
        out.sort(Comparator.comparingInt((CompanionEntity c) -> c.tickCount).reversed());
        return out;
    }

    /**
     * Park a world Bit on this parent (inventory kept in snapshot). Increases stored count.
     * @return true if stored
     */
    public boolean storeChild(CompanionEntity child) {
        if (level().isClientSide() || child == null || !child.isAlive() || isChildCompanion()) {
            return false;
        }
        if (!getUUID().equals(child.getLeaderUuid())) {
            return false;
        }
        CompoundTag entry = new CompoundTag();
        NbtUuids.put(entry, CompanionStoredChildren.ENTRY_UUID, child.getUUID());
        entry.put(CompanionStoredChildren.ENTRY_DATA, snapshotEntity(child));
        storedChildren.add(entry);
        syncStoredChildCount();
        child.discard();
        return true;
    }

    public boolean storeDyingChildSnapshot(CompanionEntity child) {
        if (level().isClientSide() || child == null || isChildCompanion()) {
            return false;
        }
        if (!getUUID().equals(child.getLeaderUuid())) {
            return false;
        }
        CompoundTag entry = new CompoundTag();
        NbtUuids.put(entry, CompanionStoredChildren.ENTRY_UUID, child.getUUID());
        entry.put(CompanionStoredChildren.ENTRY_DATA, snapshotEntity(child));
        storedChildren.add(entry);
        syncStoredChildCount();
        return true;
    }

    /** Store the oldest living Bit. Menu "Remove child". */
    public boolean storeNextLivingChild() {
        List<CompanionEntity> living = listLivingChildren();
        if (living.isEmpty()) {
            return false;
        }
        return storeChild(living.getFirst());
    }

    /** Store every living Bit (used when charm-storing the parent). */
    public int storeAllLivingChildren() {
        int stored = 0;
        for (CompanionEntity child : listLivingChildren()) {
            if (storeChild(child)) {
                stored++;
            }
        }
        return stored;
    }

    /**
     * Spawn the oldest stored Bit near this parent. Decreases stored count.
     * Respects child caps. Returns null if empty or at limit.
     */
    @Nullable
    public CompanionEntity callNextStoredChild(ServerPlayer player) {
        if (level().isClientSide() || storedChildren.isEmpty() || isChildCompanion()) {
            return null;
        }
        if (!(level() instanceof ServerLevel)) {
            return null;
        }
        if (CompanionRecruitment.countChildrenOf(player, getUUID()) >= getMaxChildren()) {
            return null;
        }
        CompoundTag entry = storedChildren.getCompoundOrEmpty(0);
        storedChildren.remove(0);
        syncStoredChildCount();
        UUID childUuid = NbtUuids.has(entry, CompanionStoredChildren.ENTRY_UUID)
                ? NbtUuids.get(entry, CompanionStoredChildren.ENTRY_UUID)
                : UUID.randomUUID();
        CompoundTag data = entry.contains(CompanionStoredChildren.ENTRY_DATA)
                ? entry.getCompoundOrEmpty(CompanionStoredChildren.ENTRY_DATA)
                : entry;
        CompanionEntity child = CompanionRecruitment.spawnStoredChild(player, this, data.copy(), childUuid);
        if (child == null) {
            storedChildren.add(0, entry);
            syncStoredChildCount();
        }
        return child;
    }

    /** Prefer {@link #storeAllLivingChildren()} so Bits can be called back. */
    public void despawnChildCompanions() {
        storeAllLivingChildren();
    }

    @Nullable
    public Player getOwner() {
        UUID owner = getOwnerUuid();
        if (owner == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(owner);
    }

    @Nullable
    public UUID getOwnerUuid() {
        String raw = entityData.get(DATA_OWNER);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String getOwnerName() {
        return ownerName == null ? "" : ownerName;
    }

    public void setOwnerName(@Nullable String name) {
        ownerName = name == null ? "" : name.trim();
    }

    public boolean isChunkLoadingEnabled() {
        return chunkLoadingEnabled;
    }

    public void setChunkLoadingEnabled(boolean enabled) {
        chunkLoadingEnabled = enabled;
        if (!enabled && !level().isClientSide()) {
            com.azscompanions.world.CompanionChunkTickets.release(this);
        }
    }

    public void addTrusted(UUID uuid) {
        trustedPlayers.add(uuid);
    }

    public Set<UUID> getTrustedPlayers() {
        return Set.copyOf(trustedPlayers);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public void setPermission(String permission, boolean enabled) {
        if (enabled) {
            permissions.add(permission);
        } else {
            permissions.remove(permission);
        }
    }

    public CompanionMode getMode() {
        return CompanionMode.byName(entityData.get(DATA_MODE));
    }

    public void setMode(CompanionMode mode) {
        entityData.set(DATA_MODE, mode.getSerializedName());
        syncSittingFromMode();
        if (mode != CompanionMode.TASK) {
            taskQueue.cancelActive("mode_changed");
        }
        if (mode == CompanionMode.FOLLOW || mode == CompanionMode.WANDER) {
            getNavigation().stop();
        }
    }

    public void syncSittingFromMode() {
        CompanionMode mode = getMode();
        entityData.set(DATA_SITTING, mode == CompanionMode.SIT || mode == CompanionMode.STAY);
    }

    public boolean isSitting() {
        return entityData.get(DATA_SITTING);
    }

    public CompanionInventory getCompanionInventory() {
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

    public TaskQueue getTaskQueue() {
        return taskQueue;
    }

    @Nullable
    public BlockPos getHomePos() {
        return homePos;
    }

    public void setHomePos(BlockPos homePos) {
        this.homePos = homePos == null ? null : homePos.immutable();
    }

    @Nullable
    public BlockPos getHomeBedPos() {
        return homeBedPos;
    }

    public void setHomeBedPos(@Nullable BlockPos homeBedPos) {
        this.homeBedPos = homeBedPos == null ? null : homeBedPos.immutable();
    }

    @Nullable
    public BlockPos getGuardCenter() {
        return guardCenter;
    }

    public void setGuardCenter(BlockPos center, int radius) {
        this.guardCenter = center.immutable();
        this.guardRadius = Math.max(2, radius);
    }

    public int getGuardRadius() {
        return guardRadius;
    }

    public String getVoiceProfile() {
        return voiceProfile;
    }

    public void setVoiceProfile(String voiceProfile) {
        this.voiceProfile = voiceProfile;
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
        return entityData.get(DATA_SLIM_ARMS);
    }

    public void setSlimArms(boolean slim) {
        entityData.set(DATA_SLIM_ARMS, slim);
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
        if (!level().isClientSide() && previous != value) {
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
        if (level().isClientSide()) {
            return;
        }
        CompanionForm form = getForm();
        int[] slots = {
                CompanionInventory.HEAD,
                CompanionInventory.CHEST,
                CompanionInventory.LEGS,
                CompanionInventory.FEET
        };
        EquipmentSlot[] uiSlots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = inventory.getStackInSlot(slots[i]);
            if (stack.isEmpty() || CompanionArmorRules.mayPlaceInArmorSlot(form, uiSlots[i], stack)) {
                continue;
            }
            inventory.setStackInSlot(slots[i], ItemStack.EMPTY);
            ItemStack leftover = inventory.insertItemAuto(stack, false);
            if (!leftover.isEmpty()) {
                this.spawnAtLocation((ServerLevel) this.level(), leftover);
            }
        }
    }

    /** Drop any Companion Charm that ended up in companion inventory/hands. */
    public void ejectForbiddenCharm() {
        if (level().isClientSide() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < CompanionInventory.TOTAL_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!CompanionCharmItem.isCharm(stack)) {
                continue;
            }
            inventory.setStackInSlot(i, ItemStack.EMPTY);
            this.spawnAtLocation(serverLevel, stack);
        }
    }

    /**
     * Form/scale are synched data — clients must refresh hitbox + {@link EntityAttachment#NAME_TAG}
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

    /** Copy spacing from a parent; Bits get a slightly tighter leash. */
    public void inheritSpacingFrom(CompanionEntity parent) {
        if (parent == null) {
            return;
        }
        float childFollow = CompanionFollowDistances.inheritFollowRadius(parent.getFollowRadius());
        setFollowRadius(childFollow);
        setPersonalSpace(CompanionFollowDistances.inheritPersonalSpace(parent.getPersonalSpace()));
        setWanderRadius(CompanionFollowDistances.inheritWanderRadius(parent.getWanderRadius(), childFollow));
    }

    public boolean wantsAggressiveTargets() {
        return CompanionCombatTargetSupport.wantsCombatTargets();
    }

    /** Prey filter for hostile attitude / team rivals — never owner or trusted. */
    public boolean isValidHostilePrey(LivingEntity target) {
        return CompanionCombatTargetSupport.isValidHostilePrey(
                isAllowedCombatant(target),
                ProtectionHelper.isProtectedEntity(target),
                isTeamRival(target),
                getAttitude().isHostile(),
                target.getType().getCategory().isFriendly(),
                target instanceof Player);
    }

    private boolean isAllowedCombatant(LivingEntity target) {
        if (target == null || !target.isAlive() || target == this) {
            return false;
        }
        if (target instanceof Player player && (isOwnedBy(player) || isTrusted(player))) {
            return false;
        }
        if (target instanceof CompanionEntity other) {
            if (CompanionTeamColors.sameTeam(getTeamId(), other.getTeamId())) {
                return false;
            }
        }
        if (target instanceof OwnableEntity ownable) {
            UUID petOwner = null;
            var ref = ownable.getOwnerReference();
            if (ref != null) {
                petOwner = ref.getUUID();
            }
            if (petOwner != null && (petOwner.equals(getOwnerUuid()) || trustedPlayers.contains(petOwner))) {
                // Same-owner pets: only fight other teammates when both have rival teams.
                return target instanceof CompanionEntity && isTeamRival(target);
            }
        }
        return true;
    }

    private boolean isTeamRival(LivingEntity target) {
        if (!(target instanceof CompanionEntity other)) {
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

    /**
     * Sets companion body scale (0.5–3.0). Value is applied 1:1 to {@link Attributes#SCALE}
     * so hitbox/collision and rendering stay in sync. Default for new companions is 0.7.
     */
    public void setBodyScale(float scale) {
        float clamped = Math.max(MIN_BODY_SCALE, Math.min(MAX_BODY_SCALE, scale));
        entityData.set(DATA_BODY_SCALE, clamped);
        var attr = getAttribute(Attributes.SCALE);
        if (attr != null) {
            attr.setBaseValue(clamped);
        }
        refreshDimensions();
        if (!level().isClientSide()) {
            MisterWigglySidekick.syncScaleFromCompanion(this);
        }
    }

    public String getPronouns() {
        return pronouns;
    }

    public void setPronouns(String pronouns) {
        this.pronouns = pronouns;
    }

    public String getBehaviorStyle() {
        return behaviorStyle;
    }

    public void setCustomDisplayName(String name) {
        boolean wasKon = isKonNamed();
        String trimmed = name == null ? "" : name.trim();
        entityData.set(DATA_CUSTOM_NAME_OVERRIDE, trimmed);
        if (!trimmed.isEmpty()) {
            setCustomName(Component.literal(trimmed));
        }
        if (isKonNamed() && !wasKon) {
            applyKonSpecialDefaults();
        }
    }

    /** Case-insensitive check: companion is the special Kon character. */
    public boolean isKonNamed() {
        String override = entityData.get(DATA_CUSTOM_NAME_OVERRIDE);
        if (override != null && !override.isBlank()) {
            return override.trim().equalsIgnoreCase("Kon");
        }
        return false;
    }

    /**
     * First charm summon defaults: player's username + player's skin reference.
     * If the player is literally named Kon, Kon special defaults apply instead.
     */
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
        String playerName = player.getGameProfile().name();
        setCustomDisplayName(playerName);
        if (!isKonNamed()) {
            setSkinPath("player:" + player.getUUID());
        }
    }

    /** Kon skin + one-time Kon Bed grant when the companion becomes named Kon. */
    public void applyKonSpecialDefaults() {
        CompanionDefinition def = CompanionRegistry.getOrKon(CompanionRegistry.KON_ID);
        setSkinPath(def.defaultSkin().toString());
        grantKonBedToOwnerOnce();
    }

    private void grantKonBedToOwnerOnce() {
        if (konBedGranted || level().isClientSide()) {
            return;
        }
        if (!(getOwner() instanceof ServerPlayer player)) {
            return;
        }
        konBedGranted = true;
        ItemStack bed = new ItemStack(ModItems.KON_BED.get());
        if (!player.getInventory().add(bed)) {
            player.drop(bed, false);
        }
        player.sendSystemMessage(Component.translatable("message.azscompanions.kon_bed_granted"));
    }

    public float getBust() {
        return entityData.get(DATA_BUST);
    }

    public void setBust(float value) {
        entityData.set(DATA_BUST, CompanionBodyProportions.clampBust(value));
    }

    public float getWaist() {
        return entityData.get(DATA_WAIST);
    }

    public void setWaist(float value) {
        entityData.set(DATA_WAIST, CompanionBodyProportions.clampWaist(value));
    }

    public float getHips() {
        return entityData.get(DATA_HIPS);
    }

    public void setHips(float value) {
        entityData.set(DATA_HIPS, CompanionBodyProportions.clampHips(value));
    }

    public float getShoulders() {
        return entityData.get(DATA_SHOULDERS);
    }

    public void setShoulders(float value) {
        entityData.set(DATA_SHOULDERS, CompanionBodyProportions.clampShoulders(value));
    }

    public float getBustOffset() {
        return entityData.get(DATA_BUST_OFFSET);
    }

    public void setBustOffset(float value) {
        entityData.set(DATA_BUST_OFFSET, CompanionBodyProportions.clampBustOffset(value));
    }

    public void resetProportionsToDefaults() {
        setBust(CompanionBodyProportions.DEFAULT_BUST);
        setWaist(CompanionBodyProportions.DEFAULT_WAIST);
        setHips(CompanionBodyProportions.DEFAULT_HIPS);
        setShoulders(CompanionBodyProportions.DEFAULT_SHOULDERS);
        setBustOffset(CompanionBodyProportions.DEFAULT_BUST_OFFSET);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        UUID owner = getOwnerUuid();
        if (owner != null) {
            output.store("Owner", UUIDUtil.CODEC, owner);
        }
        if (!getOwnerName().isBlank()) {
            output.putString("OwnerName", getOwnerName());
        }
        output.putBoolean("ChunkLoading", chunkLoadingEnabled);
        if (leaderUuid != null) {
            output.store("LeaderUuid", UUIDUtil.CODEC, leaderUuid);
        }
        output.putBoolean("FightSpawn", fightSpawn);
        output.putBoolean(CompanionCciSummonSupport.NBT_SUMMONED, cciSummoned);
        output.putLong(CompanionCciSummonSupport.NBT_EXPIRE_AT, cciExpireAtGameTime);
        if (cciMaxHealth > 0.0f) {
            output.putFloat(CompanionCciSummonSupport.NBT_MAX_HEALTH, cciMaxHealth);
        }
        output.putString("Definition", entityData.get(DATA_DEFINITION));
        output.putString("Mode", entityData.get(DATA_MODE));
        output.putString("VoiceProfile", voiceProfile);
        output.putString("SkinPath", getSkinPath());
        output.putString("SkinSleeping", getSleepingSkinPath());
        output.putString("SkinBathing", getBathingSkinPath());
        output.putString("SkinAdventuring", getAdventuringSkinPath());
        output.putBoolean("SlimArms", isSlimArms());
        output.putString("Gender", getGender().getSerializedName());
        output.putBoolean("KonBedGranted", konBedGranted);
        output.putFloat("BodyScale", getBodyScale());
        output.putFloat("Bust", getBust());
        output.putFloat("Waist", getWaist());
        output.putFloat("Hips", getHips());
        output.putFloat("Shoulders", getShoulders());
        output.putFloat("BustOffset", getBustOffset());
        output.putString("Pronouns", pronouns);
        output.putString("BehaviorStyle", behaviorStyle);
        output.putString(com.azscompanions.ai.CompanionPersona.NBT_WHO, getPersona().whoAmI());
        output.putString(com.azscompanions.ai.CompanionPersona.NBT_WHAT, getPersona().whatAmIDoing());
        output.putString(com.azscompanions.ai.CompanionPersona.NBT_HOW, getPersona().howWillIBe());
        output.putString(com.azscompanions.ai.CompanionPersona.NBT_SPEECH, getPersona().speechStyle());
        output.putString(com.azscompanions.ai.CompanionPersona.NBT_RELATIONSHIP, getPersona().relationshipToOwner());
        output.putString(com.azscompanions.ai.CompanionPersona.NBT_QUIRKS, getPersona().quirks());
        output.putBoolean(com.azscompanions.ai.CompanionPersona.NBT_INITIALIZED, getPersona().initialized());
        output.putString("CustomNameOverride", entityData.get(DATA_CUSTOM_NAME_OVERRIDE));
        output.putString("CompanionForm", getForm().serializedName());
        output.putString(CompanionFormVariants.NBT_KEY, getFormVariant());
        output.putBoolean("ShowNameTag", isNameTagVisible());
        output.putBoolean("ShowArmor", isArmorVisible());
        output.putString("Attitude", getAttitude().serializedName());
        output.putString("TeamId", getTeamId() == null ? "" : getTeamId());
        output.putFloat("FollowRadius", getFollowRadius());
        output.putFloat("PersonalSpace", getPersonalSpace());
        output.putFloat("WanderRadius", getWanderRadius());
        output.store("Inventory", CompoundTag.CODEC, inventory.save(level().registryAccess()));
        output.store("Tasks", CompoundTag.CODEC, taskQueue.save());
        if (homePos != null) {
            output.putLong("HomePos", homePos.asLong());
        }
        if (homeBedPos != null) {
            output.putLong("HomeBedPos", homeBedPos.asLong());
        }
        if (guardCenter != null) {
            output.putLong("GuardCenter", guardCenter.asLong());
            output.putInt("GuardRadius", guardRadius);
        }
        ValueOutput trustOut = output.child("Trusted");
        int i = 0;
        for (UUID uuid : trustedPlayers) {
            trustOut.store("t" + i++, UUIDUtil.CODEC, uuid);
        }
        output.putInt("TrustedCount", i);
        output.putString("Permissions", String.join(",", permissions));
        if (!offeredFlower.isEmpty()) {
            output.store("OfferedFlower", ItemStack.CODEC, offeredFlower);
        }
        CompoundTag storedBag = new CompoundTag();
        storedBag.put(CompanionStoredChildren.NBT_LIST, storedChildren.copy());
        output.store("StoredChildrenBag", CompoundTag.CODEC, storedBag);
        output.putInt("MaxChildren", getMaxChildren());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // Keep existing companions at player-like 20 HP max.
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null && maxHealth.getBaseValue() != 20.0d) {
            maxHealth.setBaseValue(20.0d);
            if (getHealth() > 20.0f) {
                setHealth(20.0f);
            }
        }
        input.read("Owner", UUIDUtil.CODEC).ifPresent(this::setOwnerUuid);
        setOwnerName(input.getStringOr("OwnerName", ""));
        chunkLoadingEnabled = input.getBooleanOr("ChunkLoading", true);
        leaderUuid = input.read("LeaderUuid", UUIDUtil.CODEC).orElse(null);
        fightSpawn = input.getBooleanOr("FightSpawn", false);
        cciSummoned = input.getBooleanOr(CompanionCciSummonSupport.NBT_SUMMONED, false);
        cciExpireAtGameTime = input.getLongOr(CompanionCciSummonSupport.NBT_EXPIRE_AT, 0L);
        cciMaxHealth = input.getFloatOr(CompanionCciSummonSupport.NBT_MAX_HEALTH, 0.0f);
        if (cciSummoned && cciMaxHealth > 0.0f && maxHealth != null) {
            maxHealth.setBaseValue(cciMaxHealth);
            if (getHealth() > cciMaxHealth) {
                setHealth(cciMaxHealth);
            }
        }
        entityData.set(DATA_DEFINITION, input.getStringOr("Definition", entityData.get(DATA_DEFINITION)));
        entityData.set(DATA_MODE, input.getStringOr("Mode", entityData.get(DATA_MODE)));
        syncSittingFromMode();
        voiceProfile = input.getStringOr("VoiceProfile", "");
        setSkinPath(input.getStringOr("SkinPath", getSkinPath()));
        setSleepingSkinPath(input.getStringOr("SkinSleeping", getSleepingSkinPath()));
        setBathingSkinPath(input.getStringOr("SkinBathing", getBathingSkinPath()));
        setAdventuringSkinPath(input.getStringOr("SkinAdventuring", getAdventuringSkinPath()));
        setSlimArms(input.getBooleanOr("SlimArms", isSlimArms()));
        setGender(CompanionGender.byName(input.getStringOr("Gender", CompanionGender.FEMALE.getSerializedName())));
        konBedGranted = input.getBooleanOr("KonBedGranted", false);
        setBodyScale(input.getFloatOr("BodyScale", DEFAULT_BODY_SCALE));
        setBust(input.getFloatOr("Bust", CompanionBodyProportions.DEFAULT_BUST));
        setWaist(input.getFloatOr("Waist", CompanionBodyProportions.DEFAULT_WAIST));
        setHips(input.getFloatOr("Hips", CompanionBodyProportions.DEFAULT_HIPS));
        setShoulders(input.getFloatOr("Shoulders", CompanionBodyProportions.DEFAULT_SHOULDERS));
        setBustOffset(input.getFloatOr("BustOffset", CompanionBodyProportions.DEFAULT_BUST_OFFSET));
        pronouns = input.getStringOr("Pronouns", "");
        behaviorStyle = input.getStringOr("BehaviorStyle", "");
        persona = new com.azscompanions.ai.CompanionPersona(
                input.getStringOr(com.azscompanions.ai.CompanionPersona.NBT_WHO, ""),
                input.getStringOr(com.azscompanions.ai.CompanionPersona.NBT_WHAT, ""),
                input.getStringOr(com.azscompanions.ai.CompanionPersona.NBT_HOW, ""),
                input.getStringOr(com.azscompanions.ai.CompanionPersona.NBT_SPEECH, ""),
                input.getStringOr(com.azscompanions.ai.CompanionPersona.NBT_RELATIONSHIP, ""),
                input.getStringOr(com.azscompanions.ai.CompanionPersona.NBT_QUIRKS, ""),
                input.getBooleanOr(com.azscompanions.ai.CompanionPersona.NBT_INITIALIZED, false)
        );
        String override = input.getStringOr("CustomNameOverride", "");
        entityData.set(DATA_CUSTOM_NAME_OVERRIDE, override);
        if (!override.isEmpty()) {
            setCustomName(Component.literal(override));
        }
        setForm(CompanionForm.byName(input.getStringOr("CompanionForm", CompanionForm.PLAYER.serializedName())));
        String savedVariant = input.getStringOr(CompanionFormVariants.NBT_KEY, "");
        if (!savedVariant.isEmpty()) {
            setFormVariant(savedVariant);
        } else if (getForm() == CompanionForm.WOLF
                && WolfyPerkSupport.isWolfyName(getChatDisplayName())) {
            setFormVariant(WolfyPerkSupport.BROWN_WOLF_VARIANT_ID);
        } else {
            setFormVariant(CompanionFormVariants.defaultVariant(getForm()));
        }
        setNameTagVisible(input.getBooleanOr("ShowNameTag", true));
        setArmorVisible(input.getBooleanOr("ShowArmor", true));
        setAttitude(CompanionAttitude.byName(input.getStringOr("Attitude", CompanionAttitude.PASSIVE.serializedName())));
        setTeamId(input.getStringOr("TeamId", ""));
        setFollowRadius(input.getFloatOr("FollowRadius", CompanionFollowDistances.DEFAULT_FOLLOW_RADIUS));
        setPersonalSpace(input.getFloatOr("PersonalSpace", CompanionFollowDistances.DEFAULT_PERSONAL_SPACE));
        setWanderRadius(input.getFloatOr("WanderRadius", CompanionFollowDistances.DEFAULT_WANDER_RADIUS));
        input.read("Inventory", CompoundTag.CODEC).ifPresent(inv -> inventory.load(inv, level().registryAccess()));
        ejectIncompatibleArmor();
        ejectForbiddenCharm();
        input.read("Tasks", CompoundTag.CODEC).ifPresent(taskQueue::load);
        input.getLong("HomePos").ifPresent(v -> homePos = BlockPos.of(v));
        input.getLong("HomeBedPos").ifPresentOrElse(
                v -> homeBedPos = BlockPos.of(v),
                () -> {
                    if (homePos != null) {
                        homeBedPos = homePos;
                    }
                });
        input.getLong("GuardCenter").ifPresent(v -> {
            guardCenter = BlockPos.of(v);
            guardRadius = input.getIntOr("GuardRadius", 0);
        });
        trustedPlayers.clear();
        ValueInput trustIn = input.childOrEmpty("Trusted");
        int count = input.getIntOr("TrustedCount", 0);
        for (int i = 0; i < count; i++) {
            trustIn.read("t" + i, UUIDUtil.CODEC).ifPresent(trustedPlayers::add);
        }
        permissions.clear();
        for (String permission : input.getStringOr("Permissions", "").split(",")) {
            if (!permission.isBlank()) {
                permissions.add(permission.trim());
            }
        }
        offeredFlower = input.read("OfferedFlower", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        storedChildren.clear();
        input.read("StoredChildrenBag", CompoundTag.CODEC).ifPresent(bag -> {
            ListTag list = bag.getListOrEmpty(CompanionStoredChildren.NBT_LIST);
            for (int i = 0; i < list.size(); i++) {
                storedChildren.add(list.getCompoundOrEmpty(i).copy());
            }
        });
        syncStoredChildCount();
        setMaxChildren(input.getIntOr("MaxChildren", ServerConfig.MAX_CHILD_COMPANIONS_PER_LEADER.get()));
        if (!level().isClientSide() && getOwnerUuid() != null && !applyingPlayerPersistentData) {
            CompanionPlayerDataSupport.apply(this);
        }
    }

}
