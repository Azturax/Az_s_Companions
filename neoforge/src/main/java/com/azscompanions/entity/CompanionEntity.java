package com.azscompanions.entity;

import com.azscompanions.ai.ChildAutonomyMode;
import com.azscompanions.ai.CompanionAiActionTrust;
import com.azscompanions.ai.CompanionAiAsk;
import com.azscompanions.ai.CompanionAiChatSupport;
import com.azscompanions.ai.CompanionChatFormat;
import com.azscompanions.ai.CompanionAiRuntime;
import com.azscompanions.ai.CompanionAiSettings;
import com.azscompanions.ai.CompanionChatCensor;
import com.azscompanions.ai.CompanionRecentAction;
import com.azscompanions.ai.CompanionChatEventSupport;
import com.azscompanions.ai.CompanionRecentActionMemory;
import com.azscompanions.compat.hosted.IntegratedMultiplayerCompat;
import com.azscompanions.compat.hosted.PlayerIdentityCompat;
import com.azscompanions.config.CommonConfig;
import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionPlayMode;
import com.azscompanions.entity.ai.CompanionFollowGoal;
import com.azscompanions.entity.ai.CompanionHostileTargetGoal;
import com.azscompanions.entity.ai.CompanionLookAtOwnerGoal;
import com.azscompanions.entity.ai.CompanionMeleeAttackGoal;
import com.azscompanions.entity.ai.CompanionOwnerDefendTargetGoal;
import com.azscompanions.entity.ai.CompanionPotionBehaviorGoal;
import com.azscompanions.entity.ai.CompanionRideAlongGoal;
import com.azscompanions.entity.ai.CompanionSitGoal;
import com.azscompanions.entity.ai.CompanionSleepInBedGoal;
import com.azscompanions.entity.ai.CompanionWanderMobInteractGoal;
import com.azscompanions.entity.ai.CompanionWanderNearOwnerGoal;
import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.item.CompanionCharmItem;
import com.azscompanions.menu.CompanionInventoryMenu;
import com.azscompanions.menu.CompanionManagementMenu;
import com.azscompanions.network.packet.OpenCompanionMenuPacket;
import com.azscompanions.perk.MisterWigglySidekick;
import com.azscompanions.perk.SpecialPlayerPerks;
import com.azscompanions.perk.WolfyPerkSupport;
import com.azscompanions.registry.ModItems;
import com.azscompanions.task.TaskQueue;
import com.azscompanions.util.CompanionArmorRules;
import com.azscompanions.util.CompanionPotionHelper;
import com.azscompanions.util.ProtectionHelper;
import com.azscompanions.voice.DialogueCategory;
import com.azscompanions.voice.VoiceService;
import com.azscompanions.world.CompanionChunkTickets;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adult companion NPC. Never damages its owner, trusted players, pets, or protected targets.
 */
public class CompanionEntity extends PathfinderMob implements RangedAttackMob {
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
    /** Synced activity context id for client skin resolve ({@code sleeping}/{@code bathing}/{@code adventuring}). */
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
    private static final EntityDataAccessor<Boolean> DATA_GLOBAL_TALK =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IDLE_CHAT =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TELEPORT =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_CHAT_LISTEN =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.STRING);
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
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.OPTIONAL_UUID);
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
    /** Last-known owner profile name (NBT); hosted-world UUID remap fallback. */
    private String ownerName = "";
    private String pronouns = "she/her";
    private String behaviorStyle = "gentle";
    /** Once Kon-identity bed grant has been given to the owner. */
    private boolean konBedGranted;
    /** Transient playful “turn evil” countdown (ticks). Not persisted. */
    private int playfulEvilTicks;
    /** Duration set at activate — used for playful-evil burst timing. */
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
    /** Guard so player-store apply does not recurse through {@link #readAdditionalSaveData}. */
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
        // Never randomly drop companion inventory/equipment (hand/armor map into CompanionInventory).
        Arrays.fill(this.handDropChances, 0.0f);
        Arrays.fill(this.armorDropChances, 0.0f);
        inventory.setPersistenceHook(() -> CompanionPlayerDataSupport.save(this));
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        // Vanilla loot vacuum must not scoop harmful/neutral potions; AI goal only targets beneficial.
        if (CompanionPotionHelper.isPotionItem(stack)) {
            return CompanionPotionHelper.isAutoPickupAllowed(stack);
        }
        return super.wantsToPickUp(stack);
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
        builder.define(DATA_GLOBAL_TALK, true);
        builder.define(DATA_IDLE_CHAT, true);
        builder.define(DATA_TELEPORT, true);
        builder.define(DATA_CHAT_LISTEN, CompanionPlayerAiPrefs.defaultChatListen().configName());
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
        // Sit/stay stop movement; combat/potions/sleep outrank follow; wander when owner idle nearby.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CompanionSitGoal(this));
        goalSelector.addGoal(2, new CompanionSleepInBedGoal(this));
        goalSelector.addGoal(3, new CompanionPotionBehaviorGoal(this));
        goalSelector.addGoal(4, new RangedBowAttackGoal<>(this, CompanionBowCombatSupport.BOW_MOVE_SPEED,
                CompanionBowCombatSupport.BOW_ATTACK_INTERVAL_TICKS, CompanionBowCombatSupport.BOW_ATTACK_RADIUS));
        goalSelector.addGoal(5, new CompanionMeleeAttackGoal(this, 1.25d, true));
        goalSelector.addGoal(6, new CompanionRideAlongGoal(this));
        goalSelector.addGoal(7, new CompanionFollowGoal(this));
        goalSelector.addGoal(8, new CompanionWanderMobInteractGoal(this));
        goalSelector.addGoal(9, new CompanionWanderNearOwnerGoal(this));
        goalSelector.addGoal(10, new OpenDoorGoal(this, true));
        goalSelector.addGoal(11, new CompanionLookAtOwnerGoal(this));
        goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(13, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new CompanionOwnerDefendTargetGoal(this));
        targetSelector.addGoal(2, new CompanionHostileTargetGoal(this));
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
            if (getMode() == CompanionMode.TASK) {
                com.azscompanions.util.CompanionTorchHelper.tickWhileTasking(this, serverLevel);
            }
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
            CompanionChunkTickets.tick(this, serverLevel);
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
        if (isPassenger()) {
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
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.CAT_PURR, SoundSource.NEUTRAL, 0.55f, 0.95f + random.nextFloat() * 0.15f);
    }

    private void tickOwnerActivity() {
        Player owner = getOwner();
        if (owner == null) {
            ownerActivity.reset();
            return;
        }
        ownerActivity.tick(owner.getX(), owner.getZ());
    }

    /** Sync active player-form context so clients apply sleeping/bathing/adventuring outfits. */
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
        if (!hasReactive && !isIdleChatEnabled()) {
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
        if (!level().isClientSide) {
            CompanionPlayerDataSupport.save(this);
            MisterWigglySidekick.despawnFor(this);
            CompanionChunkTickets.release(this);
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
    public void kill() {
        if (isFullyInvincible()) {
            setHealth(getMaxHealth());
            return;
        }
        super.kill();
    }

    @Override
    public void die(DamageSource source) {
        if (isFullyInvincible()) {
            setHealth(getMaxHealth());
            return;
        }
        super.die(source);
    }

    /** Allow following the owner through vanilla and modded dimensions. */
    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return true;
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
            if (tickCount % com.azscompanions.ai.CompanionAiChatSupport.LOW_HEALTH_SPEAK_INTERVAL_TICKS == 0) {
                speak(DialogueCategory.LOW_HEALTH);
            }
            if (CommonConfig.ENABLE_HEALING_SYSTEM.get()) {
                tryEatFood();
            }
        }
        if (inventory.isFull() && tickCount % com.azscompanions.ai.CompanionAiChatSupport.INVENTORY_FULL_SPEAK_INTERVAL_TICKS == 0) {
            speak(DialogueCategory.INVENTORY_FULL);
        }
    }

    private void tryEatFood() {
        ItemStack food = inventory.getFoodSlot();
        if (!food.isEmpty() && food.getFoodProperties(this) != null) {
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

    private void tickCciSummonExpiry(ServerLevel serverLevel) {
        if (!CompanionCciSummonSupport.shouldExpire(cciSummoned, cciExpireAtGameTime, serverLevel.getGameTime())) {
            return;
        }
        setInvulnerable(false);
        hurt(damageSources().genericKill(), Float.MAX_VALUE);
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
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!isOwnedBy(player) && !isTrusted(player)) {
            player.displayClientMessage(Component.translatable("message.azscompanions.not_owner"), true);
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
        CompanionEntity child = CompanionRecruitment.spawnChild(player, this);
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
        return !stack.isEmpty() && stack.getFoodProperties(this) != null;
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
            player.displayClientMessage(Component.translatable("message.azscompanions.hand_given_main"), true);
            return InteractionResult.CONSUME;
        }
        if (off.isEmpty()) {
            inventory.setStackInSlot(CompanionInventory.OFF_HAND, held.copy());
            player.setItemInHand(hand, ItemStack.EMPTY);
            player.displayClientMessage(Component.translatable("message.azscompanions.hand_given_off"), true);
            return InteractionResult.CONSUME;
        }
        // Both occupied — swap with main hand and return old main to player.
        ItemStack previous = main.copy();
        inventory.setStackInSlot(CompanionInventory.MAIN_HAND, held.copy());
        player.setItemInHand(hand, previous);
        player.displayClientMessage(Component.translatable("message.azscompanions.hand_swapped"), true);
        return InteractionResult.CONSUME;
    }

    /** Empty-hand take: main hand first, then offhand. */
    private InteractionResult takeItemFromHands(ServerPlayer player, InteractionHand hand) {
        ItemStack main = inventory.getMainHand();
        if (!main.isEmpty()) {
            player.setItemInHand(hand, main.copy());
            inventory.setStackInSlot(CompanionInventory.MAIN_HAND, ItemStack.EMPTY);
            player.displayClientMessage(Component.translatable("message.azscompanions.hand_taken_main"), true);
            return InteractionResult.CONSUME;
        }
        ItemStack off = inventory.getOffHand();
        if (!off.isEmpty()) {
            player.setItemInHand(hand, off.copy());
            inventory.setStackInSlot(CompanionInventory.OFF_HAND, ItemStack.EMPTY);
            player.displayClientMessage(Component.translatable("message.azscompanions.hand_taken_off"), true);
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
            if (!level().isClientSide && !stack.isEmpty()) {
                this.spawnAtLocation(stack.copy());
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
    public boolean isInvulnerableTo(DamageSource source) {
        if (isFullyInvincible()) {
            return true;
        }
        if (CompanionHazardImmunity.ignores(source.typeHolder().unwrapKey()
                .map(key -> key.location().getPath())
                .orElse(""))) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isFullyInvincible()) {
            setHealth(getMaxHealth());
            return false;
        }
        if (source.getEntity() instanceof Player player && (isOwnedBy(player) || isTrusted(player))) {
            return false;
        }
        if (isInvulnerableTo(source)) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            speak(DialogueCategory.DANGER);
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (!(target instanceof LivingEntity living)) {
            return false;
        }
        if (!canAttackTarget(living)) {
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

    public boolean canAttackTarget(LivingEntity target) {
        boolean hurtLink = target.getLastHurtByMob() == this
                || target.getLastHurtByMob() == getOwner()
                || getLastHurtByMob() == target;
        return CompanionCombatTargetSupport.canAttackAcquiredTarget(
                ServerConfig.ALLOW_COMBAT.get() && hasPermission("combat"),
                isAllowedCombatant(target),
                ProtectionHelper.isProtectedEntity(target),
                isTeamRival(target),
                getAttitude().isHostile(),
                target.getType().getCategory().isFriendly(),
                ServerConfig.ATTACK_NEUTRALS_ONLY_IF_HIT.get(),
                hurtLink);
    }

    public boolean canBreakBlock(BlockPos pos) {
        if (!ServerConfig.ALLOW_GRIEFING.get() && ProtectionHelper.isProtectedBlock(level(), pos, getOwner())) {
            return false;
        }
        return ProtectionHelper.canCompanionModify(level(), pos, this);
    }

    /** Prefer bow/crossbow ranged combat when humanoid form + equipped with ammo (or Infinity). */
    public boolean shouldPreferBowCombat() {
        if (!CompanionBowCombatSupport.formCanUseBow(getForm())) {
            return false;
        }
        ItemStack weapon = getMainHandItem();
        if (weapon.isEmpty()) {
            weapon = getOffhandItem();
        }
        if (weapon.isEmpty()) {
            return false;
        }
        String id = BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString();
        boolean bow = CompanionBowCombatSupport.isBowItemId(id) || weapon.getItem() instanceof BowItem
                || weapon.getItem() instanceof ProjectileWeaponItem;
        if (!bow) {
            return false;
        }
        boolean infinity = hasInfinityEnchant(weapon);
        return CompanionBowCombatSupport.shouldPreferRanged(true, true, infinity, hasArrowAmmo());
    }

    private boolean hasInfinityEnchant(ItemStack weapon) {
        if (level().registryAccess() == null) {
            return false;
        }
        var infinity = level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .get(Enchantments.INFINITY);
        return infinity.isPresent() && EnchantmentHelper.getItemEnchantmentLevel(infinity.get(), weapon) > 0;
    }

    private boolean hasArrowAmmo() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (CompanionBowCombatSupport.isArrowItemId(id) || stack.is(Items.ARROW)
                    || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private ItemStack findAndConsumeArrow(boolean infinity) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (!(CompanionBowCombatSupport.isArrowItemId(id) || stack.is(Items.ARROW)
                    || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW))) {
                continue;
            }
            ItemStack arrow = stack.copyWithCount(1);
            if (!infinity) {
                stack.shrink(1);
                inventory.setStackInSlot(i, stack);
            }
            return arrow;
        }
        return infinity ? new ItemStack(Items.ARROW) : null;
    }

    @Override
    public boolean canFireProjectileWeapon(ProjectileWeaponItem weapon) {
        return shouldPreferBowCombat();
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        if (!CompanionBowCombatSupport.formCanUseBow(getForm())) {
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel) || target == null || !canAttackTarget(target)) {
            return;
        }
        ItemStack weapon = getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem
                || item instanceof ProjectileWeaponItem
                || CompanionBowCombatSupport.isBowItemId(BuiltInRegistries.ITEM.getKey(item).toString())));
        if (weapon.isEmpty()) {
            weapon = getMainHandItem();
        }
        boolean infinity = hasInfinityEnchant(weapon);
        ItemStack ammo = findAndConsumeArrow(infinity);
        if (ammo == null || ammo.isEmpty()) {
            return;
        }
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, velocity, weapon);
        double dx = target.getX() - getX();
        double dy = target.getY(0.3333333333333333d) - arrow.getY();
        double dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + dist * 0.2d, dz, 1.6f, 14 - level().getDifficulty().getId() * 4);
        playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (getRandom().nextFloat() * 0.4f + 0.8f));
        serverLevel.addFreshEntity(arrow);
    }

    @Override
    protected void dropEquipment() {
        if (CompanionInventoryPersistence.shouldKeepInventoryOnDeath(ServerConfig.KEEP_INVENTORY_ON_DEATH.get())) {
            return;
        }
        super.dropEquipment();
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource damageSource) {
        if (CompanionInventoryPersistence.shouldKeepInventoryOnDeath(ServerConfig.KEEP_INVENTORY_ON_DEATH.get())) {
            // Keep backpack + equipment; still allow XP if any via super skip.
            return;
        }
        super.dropAllDeathLoot(level, damageSource);
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
        if (level().isClientSide || line == null || line.isBlank()) {
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
            Component msg = Component.literal(CompanionChatFormat.formatLine(getOwnerUuid(), getChatDisplayName(), text));
            if (isGlobalTalkEnabled()) {
                var server = owner.getServer();
                if (server != null) {
                    server.getPlayerList().broadcastSystemMessage(msg, false);
                    return;
                }
            }
            owner.displayClientMessage(msg, false);
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
        if (level().isClientSide) {
            return;
        }
        if (!ServerConfig.COMPANION_CHAT_MESSAGES.get()) {
            return;
        }
        if (getOwner() instanceof ServerPlayer owner) {
            String line = Component.translatable(langKey).getString();
            owner.displayClientMessage(Component.literal(CompanionChatFormat.formatLine(getOwnerUuid(), getChatDisplayName(), line)), false);
        }
    }

    public CompanionDefinition getDefinition() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(DATA_DEFINITION));
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
        setOwnerName(player.getGameProfile().getName());
        trustedPlayers.add(player.getUUID());
    }

    public void setOwnerUuid(@Nullable UUID uuid) {
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

    /** Living Bits under this parent, oldest first (approx. creation order). */
    public List<CompanionEntity> listLivingChildren() {
        List<CompanionEntity> out = new ArrayList<>();
        if (level().isClientSide || !(level() instanceof ServerLevel serverLevel)) {
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

    /**
     * Snapshot a dying Bit onto this parent (inventory kept). Does not require {@link #isAlive()} on the Bit.
     */
    public boolean storeDyingChildSnapshot(CompanionEntity child) {
        if (level().isClientSide || child == null || isChildCompanion()) {
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
        if (level().isClientSide || storedChildren.isEmpty() || isChildCompanion()) {
            return null;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (CompanionRecruitment.countChildrenOf(player, getUUID()) >= getMaxChildren()) {
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
        CompanionEntity child = CompanionRecruitment.spawnStoredChild(player, this, data.copy(), childUuid);
        if (child == null) {
            // Put snapshot back if spawn failed.
            storedChildren.add(0, entry);
            syncStoredChildCount();
        }
        return child;
    }

    /** @deprecated Prefer {@link #storeAllLivingChildren()} so Bits can be called back. */
    @Deprecated
    public void despawnChildCompanions() {
        storeAllLivingChildren();
    }

    @Nullable
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

    @Nullable
    public UUID getOwnerUuid() {
        return entityData.get(DATA_OWNER).orElse(null);
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

    /** Sync sit/stay pose from Mode without cancelling tasks (used on NBT load). */
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

    /** Synced activity context id, or blank when none / not player form. */
    public String getActiveContextSkinId() {
        return entityData.get(DATA_ACTIVE_CONTEXT);
    }

    /**
     * Skin path for player-form rendering: context outfit → custom SkinPath → blank (base default).
     */
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

    public void ejectIncompatibleArmor() {
        if (level().isClientSide) {
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
                this.spawnAtLocation(leftover);
            }
        }
    }

    /** Drop any Companion Charm that ended up in companion inventory/hands. */
    public void ejectForbiddenCharm() {
        if (level().isClientSide) {
            return;
        }
        for (int i = 0; i < CompanionInventory.TOTAL_SIZE; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!CompanionCharmItem.isCharm(stack)) {
                continue;
            }
            inventory.setStackInSlot(i, ItemStack.EMPTY);
            this.spawnAtLocation(stack);
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

    public boolean isGlobalTalkEnabled() {
        return entityData.get(DATA_GLOBAL_TALK);
    }

    public void setGlobalTalkEnabled(boolean enabled) {
        entityData.set(DATA_GLOBAL_TALK, enabled);
    }

    public boolean isIdleChatEnabled() {
        return entityData.get(DATA_IDLE_CHAT);
    }

    public void setIdleChatEnabled(boolean enabled) {
        entityData.set(DATA_IDLE_CHAT, enabled);
    }

    public boolean isTeleportEnabled() {
        return entityData.get(DATA_TELEPORT);
    }

    public void setTeleportEnabled(boolean enabled) {
        entityData.set(DATA_TELEPORT, enabled);
    }

    public com.azscompanions.ai.ChatListenMode getChatListenMode() {
        return CompanionPlayerAiPrefs.parseChatListen(
                entityData.get(DATA_CHAT_LISTEN), CompanionPlayerAiPrefs.defaultChatListen());
    }

    public void setChatListenMode(com.azscompanions.ai.ChatListenMode mode) {
        entityData.set(DATA_CHAT_LISTEN,
                (mode == null ? CompanionPlayerAiPrefs.defaultChatListen() : mode).configName());
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
            CompanionChunkTickets.release(this);
        }
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

    /** Prey filter for hostile attitude / team rivals / PASSIVE monster aggro — never owner or trusted. */
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
            UUID petOwner = ownable.getOwnerUUID();
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
        if (!level().isClientSide) {
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
        String playerName = player.getGameProfile().getName();
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
        if (konBedGranted || level().isClientSide) {
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
        player.displayClientMessage(Component.translatable("message.azscompanions.kon_bed_granted"), true);
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
        tag.putBoolean(CompanionCciSummonSupport.NBT_SUMMONED, cciSummoned);
        tag.putLong(CompanionCciSummonSupport.NBT_EXPIRE_AT, cciExpireAtGameTime);
        if (cciMaxHealth > 0.0f) {
            tag.putFloat(CompanionCciSummonSupport.NBT_MAX_HEALTH, cciMaxHealth);
        }
        tag.putString("Definition", entityData.get(DATA_DEFINITION));
        tag.putString("Mode", entityData.get(DATA_MODE));
        tag.putString("VoiceProfile", voiceProfile);
        tag.putString("SkinPath", getSkinPath());
        tag.putString("SkinSleeping", getSleepingSkinPath());
        tag.putString("SkinBathing", getBathingSkinPath());
        tag.putString("SkinAdventuring", getAdventuringSkinPath());
        tag.putBoolean("SlimArms", isSlimArms());
        tag.putString("Gender", getGender().getSerializedName());
        tag.putBoolean("KonBedGranted", konBedGranted);
        tag.putFloat("BodyScale", getBodyScale());
        tag.putFloat("Bust", getBust());
        tag.putFloat("Waist", getWaist());
        tag.putFloat("Hips", getHips());
        tag.putFloat("Shoulders", getShoulders());
        tag.putFloat("BustOffset", getBustOffset());
        tag.putString("Pronouns", pronouns);
        tag.putString("BehaviorStyle", behaviorStyle);
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_WHO, getPersona().whoAmI());
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_WHAT, getPersona().whatAmIDoing());
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_HOW, getPersona().howWillIBe());
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_SPEECH, getPersona().speechStyle());
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_RELATIONSHIP, getPersona().relationshipToOwner());
        tag.putString(com.azscompanions.ai.CompanionPersona.NBT_QUIRKS, getPersona().quirks());
        tag.putBoolean(com.azscompanions.ai.CompanionPersona.NBT_INITIALIZED, getPersona().initialized());
        tag.putBoolean("ChunkLoading", chunkLoadingEnabled);
        tag.putString("CustomNameOverride", entityData.get(DATA_CUSTOM_NAME_OVERRIDE));
        tag.putString("CompanionForm", getForm().serializedName());
        tag.putString(CompanionFormVariants.NBT_KEY, getFormVariant());
        tag.putBoolean("ShowNameTag", isNameTagVisible());
        tag.putBoolean("ShowArmor", isArmorVisible());
        tag.putBoolean(CompanionPlayerAiPrefs.NBT_GLOBAL_TALK, isGlobalTalkEnabled());
        tag.putBoolean(CompanionPlayerAiPrefs.NBT_IDLE_CHAT, isIdleChatEnabled());
        tag.putBoolean(CompanionPlayerAiPrefs.NBT_TELEPORT, isTeleportEnabled());
        tag.putString(CompanionPlayerAiPrefs.NBT_CHAT_LISTEN, getChatListenMode().configName());
        tag.putString("Attitude", getAttitude().serializedName());
        tag.putString("TeamId", getTeamId() == null ? "" : getTeamId());
        tag.putFloat("FollowRadius", getFollowRadius());
        tag.putFloat("PersonalSpace", getPersonalSpace());
        tag.putFloat("WanderRadius", getWanderRadius());
        tag.put("Inventory", inventory.save(level().registryAccess()));
        tag.put(CompanionStoredChildren.NBT_LIST, storedChildren.copy());
        tag.putInt("MaxChildren", getMaxChildren());
        tag.put("Tasks", taskQueue.save());
        if (homePos != null) {
            tag.putLong("HomePos", homePos.asLong());
        }
        if (homeBedPos != null) {
            tag.putLong("HomeBedPos", homeBedPos.asLong());
        }
        if (guardCenter != null) {
            tag.putLong("GuardCenter", guardCenter.asLong());
            tag.putInt("GuardRadius", guardRadius);
        }
        CompoundTag trustTag = new CompoundTag();
        int i = 0;
        for (UUID uuid : trustedPlayers) {
            trustTag.putUUID("t" + i++, uuid);
        }
        tag.put("Trusted", trustTag);
        tag.putInt("TrustedCount", i);
        tag.putString("Permissions", String.join(",", permissions));
        if (!offeredFlower.isEmpty()) {
            tag.put("OfferedFlower", offeredFlower.save(level().registryAccess(), new CompoundTag()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Keep existing companions at player-like 20 HP max (CCI summons restore custom health below).
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
        cciSummoned = tag.contains(CompanionCciSummonSupport.NBT_SUMMONED)
                && tag.getBoolean(CompanionCciSummonSupport.NBT_SUMMONED);
        cciExpireAtGameTime = tag.contains(CompanionCciSummonSupport.NBT_EXPIRE_AT)
                ? tag.getLong(CompanionCciSummonSupport.NBT_EXPIRE_AT) : 0L;
        if (tag.contains(CompanionCciSummonSupport.NBT_MAX_HEALTH)) {
            cciMaxHealth = tag.getFloat(CompanionCciSummonSupport.NBT_MAX_HEALTH);
        }
        if (cciSummoned && cciMaxHealth > 0.0f && maxHealth != null) {
            maxHealth.setBaseValue(cciMaxHealth);
            if (getHealth() > cciMaxHealth) {
                setHealth(cciMaxHealth);
            }
        }
        if (tag.contains("Definition")) {
            entityData.set(DATA_DEFINITION, tag.getString("Definition"));
        }
        if (tag.contains("Mode")) {
            entityData.set(DATA_MODE, tag.getString("Mode"));
            syncSittingFromMode();
        }
        voiceProfile = tag.getString("VoiceProfile");
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
        if (tag.contains("SlimArms")) {
            setSlimArms(tag.getBoolean("SlimArms"));
        }
        if (tag.contains("Gender")) {
            setGender(CompanionGender.byName(tag.getString("Gender")));
        } else {
            setGender(CompanionGender.FEMALE);
        }
        konBedGranted = tag.contains("KonBedGranted") && tag.getBoolean("KonBedGranted");
        if (tag.contains("BodyScale")) {
            setBodyScale(tag.getFloat("BodyScale"));
        } else {
            setBodyScale(DEFAULT_BODY_SCALE);
        }
        setBust(tag.contains("Bust") ? tag.getFloat("Bust") : CompanionBodyProportions.DEFAULT_BUST);
        setWaist(tag.contains("Waist") ? tag.getFloat("Waist") : CompanionBodyProportions.DEFAULT_WAIST);
        setHips(tag.contains("Hips") ? tag.getFloat("Hips") : CompanionBodyProportions.DEFAULT_HIPS);
        setShoulders(tag.contains("Shoulders") ? tag.getFloat("Shoulders") : CompanionBodyProportions.DEFAULT_SHOULDERS);
        setBustOffset(tag.contains("BustOffset") ? tag.getFloat("BustOffset") : CompanionBodyProportions.DEFAULT_BUST_OFFSET);
        pronouns = tag.getString("Pronouns");
        behaviorStyle = tag.getString("BehaviorStyle");
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
        if (tag.contains("CustomNameOverride")) {
            String override = tag.getString("CustomNameOverride");
            entityData.set(DATA_CUSTOM_NAME_OVERRIDE, override);
            if (!override.isEmpty()) {
                setCustomName(Component.literal(override));
            }
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
        if (tag.contains(CompanionPlayerAiPrefs.NBT_GLOBAL_TALK)) {
            setGlobalTalkEnabled(tag.getBoolean(CompanionPlayerAiPrefs.NBT_GLOBAL_TALK));
        } else {
            setGlobalTalkEnabled(CompanionPlayerAiPrefs.defaultGlobalTalk());
        }
        if (tag.contains(CompanionPlayerAiPrefs.NBT_IDLE_CHAT)) {
            setIdleChatEnabled(tag.getBoolean(CompanionPlayerAiPrefs.NBT_IDLE_CHAT));
        } else {
            setIdleChatEnabled(CompanionPlayerAiPrefs.defaultIdleChat());
        }
        if (tag.contains(CompanionPlayerAiPrefs.NBT_TELEPORT)) {
            setTeleportEnabled(tag.getBoolean(CompanionPlayerAiPrefs.NBT_TELEPORT));
        } else {
            setTeleportEnabled(CompanionPlayerAiPrefs.defaultTeleport());
        }
        if (tag.contains(CompanionPlayerAiPrefs.NBT_CHAT_LISTEN)) {
            setChatListenMode(CompanionPlayerAiPrefs.parseChatListen(
                    tag.getString(CompanionPlayerAiPrefs.NBT_CHAT_LISTEN),
                    CompanionPlayerAiPrefs.defaultChatListen()));
        } else {
            setChatListenMode(CompanionPlayerAiPrefs.defaultChatListen());
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
        if (tag.contains("Inventory")) {
            inventory.load(tag.getCompound("Inventory"), level().registryAccess());
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
            setMaxChildren(ServerConfig.MAX_CHILD_COMPANIONS_PER_LEADER.get());
        }
        ejectIncompatibleArmor();
        ejectForbiddenCharm();
        if (tag.contains("Tasks")) {
            taskQueue.load(tag.getCompound("Tasks"));
        }
        if (tag.contains("HomePos")) {
            homePos = BlockPos.of(tag.getLong("HomePos"));
        }
        if (tag.contains("HomeBedPos")) {
            homeBedPos = BlockPos.of(tag.getLong("HomeBedPos"));
        } else if (homePos != null) {
            homeBedPos = homePos;
        }
        if (tag.contains("GuardCenter")) {
            guardCenter = BlockPos.of(tag.getLong("GuardCenter"));
            guardRadius = tag.getInt("GuardRadius");
        }
        trustedPlayers.clear();
        if (tag.contains("Trusted")) {
            CompoundTag trustTag = tag.getCompound("Trusted");
            int count = tag.getInt("TrustedCount");
            for (int i = 0; i < count; i++) {
                if (trustTag.hasUUID("t" + i)) {
                    trustedPlayers.add(trustTag.getUUID("t" + i));
                }
            }
        }
        permissions.clear();
        if (tag.contains("Permissions")) {
            for (String permission : tag.getString("Permissions").split(",")) {
                if (!permission.isBlank()) {
                    permissions.add(permission.trim());
                }
            }
        }
        if (tag.contains("OfferedFlower", Tag.TAG_COMPOUND)) {
            offeredFlower = ItemStack.parse(level().registryAccess(), tag.getCompound("OfferedFlower"))
                    .orElse(ItemStack.EMPTY);
        } else {
            offeredFlower = ItemStack.EMPTY;
        }
        if (!level().isClientSide && getOwnerUuid() != null && !applyingPlayerPersistentData) {
            CompanionPlayerDataSupport.apply(this);
        }
    }

}
