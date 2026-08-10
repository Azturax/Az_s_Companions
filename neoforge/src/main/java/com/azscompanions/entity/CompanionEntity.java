package com.azscompanions.entity;

import com.azscompanions.config.CommonConfig;
import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.ai.CompanionFollowGoal;
import com.azscompanions.entity.ai.CompanionLookAtOwnerGoal;
import com.azscompanions.entity.ai.CompanionOwnerDefendTargetGoal;
import com.azscompanions.entity.ai.CompanionPotionBehaviorGoal;
import com.azscompanions.entity.ai.CompanionSitGoal;
import com.azscompanions.entity.ai.CompanionSleepInBedGoal;
import com.azscompanions.entity.ai.CompanionWanderNearOwnerGoal;
import com.azscompanions.entity.inventory.CompanionInventory;
import com.azscompanions.menu.CompanionInventoryMenu;
import com.azscompanions.menu.CompanionManagementMenu;
import com.azscompanions.network.packet.OpenCompanionMenuPacket;
import com.azscompanions.perk.MisterWigglySidekick;
import com.azscompanions.perk.SpecialPlayerPerks;
import com.azscompanions.registry.ModItems;
import com.azscompanions.task.TaskQueue;
import com.azscompanions.util.CompanionPotionHelper;
import com.azscompanions.util.ProtectionHelper;
import com.azscompanions.voice.DialogueCategory;
import com.azscompanions.voice.VoiceService;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.OwnableEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.HashSet;
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
    /** Synced so client UI ownership checks work without looking at NBT. */
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(CompanionEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final CompanionInventory inventory = new CompanionInventory();
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
    private String pronouns = "she/her";
    private String behaviorStyle = "gentle";
    /** Once Kon-identity bed grant has been given to the owner. */
    private boolean konBedGranted;
    // skinPath / bodyScale / slimArms live in synched entity data

    public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanFloat(true);
        }
        this.setCanPickUpLoot(true);
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
                .add(Attributes.ATTACK_DAMAGE, 4.0d)
                .add(Attributes.FOLLOW_RANGE, 64.0d)
                .add(Attributes.ARMOR, 2.0d)
                .add(Attributes.SCALE, DEFAULT_BODY_SCALE)
                // Clear full 1-block steps at any body scale (0.5–3); vanilla step is only 0.6.
                .add(Attributes.STEP_HEIGHT, CompanionMovementAttributes.STEP_HEIGHT)
                .add(Attributes.JUMP_STRENGTH, CompanionMovementAttributes.JUMP_STRENGTH);
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
        builder.define(DATA_SLIM_ARMS, false);
        builder.define(DATA_GENDER, CompanionGender.FEMALE.getSerializedName());
        builder.define(DATA_BUST, CompanionBodyProportions.DEFAULT_BUST);
        builder.define(DATA_WAIST, CompanionBodyProportions.DEFAULT_WAIST);
        builder.define(DATA_HIPS, CompanionBodyProportions.DEFAULT_HIPS);
        builder.define(DATA_SHOULDERS, CompanionBodyProportions.DEFAULT_SHOULDERS);
        builder.define(DATA_BUST_OFFSET, CompanionBodyProportions.DEFAULT_BUST_OFFSET);
        builder.define(DATA_OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        // Sit/stay stop movement; combat/potions/sleep outrank follow; wander when owner idle nearby.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CompanionSitGoal(this));
        goalSelector.addGoal(2, new CompanionSleepInBedGoal(this));
        goalSelector.addGoal(3, new CompanionPotionBehaviorGoal(this));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.25d, true));
        goalSelector.addGoal(5, new CompanionFollowGoal(this));
        goalSelector.addGoal(6, new CompanionWanderNearOwnerGoal(this));
        goalSelector.addGoal(7, new OpenDoorGoal(this, true));
        goalSelector.addGoal(8, new CompanionLookAtOwnerGoal(this));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new CompanionOwnerDefendTargetGoal(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && level() instanceof ServerLevel) {
            // Preserve player/CCI command modes; only clear leftover task-queue modes.
            CompanionMode mode = getMode();
            if (mode != CompanionMode.FOLLOW
                    && mode != CompanionMode.SIT
                    && mode != CompanionMode.STAY
                    && mode != CompanionMode.WANDER) {
                setMode(CompanionMode.FOLLOW);
            }
            if (taskQueue.getActive() != null || !taskQueue.queued().isEmpty()) {
                taskQueue.clear();
            }
            SpecialPlayerPerks.applyCompanionPerks(this, getOwnerUuid());
            tickOwnerActivity();
            tickHomeBedLeash();
            tickSleepPurr();
            if (tickCount % 40 == 0) {
                MisterWigglySidekick.ensureFor(this);
            }
            tickStuckRecovery();
            tickSurvival();
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
        if (CompanionFollowDistances.tooCloseToTeleport(dist)) {
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
        if (!level().isClientSide) {
            MisterWigglySidekick.despawnFor(this);
        }
        super.remove(reason);
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
                if (!CompanionFollowDistances.tooCloseToTeleport(dist)
                        && CompanionFollowDistances.shouldGroundTeleport(dist)
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
            if (tickCount % 100 == 0) {
                speak(DialogueCategory.LOW_HEALTH);
            }
            if (CommonConfig.ENABLE_HEALING_SYSTEM.get()) {
                tryEatFood();
            }
        }
        if (inventory.isFull() && tickCount % 200 == 0) {
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
        // Keep personal space — land in the preferred follow ring, not on the owner's feet.
        int ring = (int) Math.round(CompanionFollowDistances.PREFERRED_DISTANCE);
        for (int i = 0; i < 12; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0d;
            int ox = (int) Math.round(Math.cos(angle) * ring);
            int oz = (int) Math.round(Math.sin(angle) * ring);
            BlockPos candidate = target.offset(ox, 0, oz);
            if (level().getBlockState(candidate).isAir() && level().getBlockState(candidate.below()).isSolid()) {
                teleportTo(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
                return;
            }
        }
        teleportTo(target.getX() + ring + 0.5, target.getY(), target.getZ() + 0.5);
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

        // Shift + right-click opens shared menu (Customize | Command | Inventory).
        if (player.isShiftKeyDown()) {
            PacketDistributor.sendToPlayer(serverPlayer, new OpenCompanionMenuPacket(getId()));
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            if (isEdibleFood(held)) {
                return feedFromPlayer(serverPlayer, hand);
            }
            return giveItemToHands(serverPlayer, hand, held);
        }
        return takeItemFromHands(serverPlayer, hand);
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

    /** Hand slots are backed by companion inventory so items render and persist with charm NBT. */
    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return inventory.getMainHand();
        }
        if (slot == EquipmentSlot.OFFHAND) {
            return inventory.getOffHand();
        }
        return super.getItemBySlot(slot);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            inventory.setStackInSlot(CompanionInventory.MAIN_HAND, stack);
            return;
        }
        if (slot == EquipmentSlot.OFFHAND) {
            inventory.setStackInSlot(CompanionInventory.OFF_HAND, stack);
            return;
        }
        super.setItemSlot(slot, stack);
    }

    public void openManagement(ServerPlayer player) {
        player.openMenu(new CompanionManagementMenu.Provider(this), buf -> buf.writeVarInt(getId()));
    }

    public void openInventory(ServerPlayer player) {
        player.openMenu(new CompanionInventoryMenu.Provider(this), buf -> buf.writeVarInt(getId()));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (CompanionHazardImmunity.ignores(source.typeHolder().unwrapKey()
                .map(key -> key.location().getPath())
                .orElse(""))) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
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
        return super.doHurtTarget(target);
    }

    public boolean canAttackTarget(LivingEntity target) {
        if (!ServerConfig.ALLOW_COMBAT.get() || !hasPermission("combat")) {
            return false;
        }
        if (target instanceof Player player && (isOwnedBy(player) || isTrusted(player))) {
            return false;
        }
        if (target instanceof OwnableEntity ownable) {
            UUID petOwner = ownable.getOwnerUUID();
            if (petOwner != null && (petOwner.equals(getOwnerUuid()) || trustedPlayers.contains(petOwner))) {
                return false;
            }
        }
        if (ProtectionHelper.isProtectedEntity(target)) {
            return false;
        }
        if (ServerConfig.ATTACK_NEUTRALS_ONLY_IF_HIT.get() && !target.getType().getCategory().isFriendly()) {
            // Hostile mobs are always fair game when combat is allowed.
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

    public void speak(DialogueCategory category) {
        CompanionDefinition definition = getDefinition();
        definition.dialogue().pick(category.lines(definition.dialogue()), random)
                .ifPresent(line -> VoiceService.get().speak(this, category, line));
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
            owner.displayClientMessage(Component.literal("<" + getChatDisplayName() + "> " + line), false);
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
        UUID owner = getOwnerUuid();
        return owner != null && owner.equals(player.getUUID());
    }

    public boolean isTrusted(Player player) {
        return trustedPlayers.contains(player.getUUID());
    }

    public void setOwner(Player player) {
        setOwnerUuid(player.getUUID());
        trustedPlayers.add(player.getUUID());
    }

    public void setOwnerUuid(@Nullable UUID uuid) {
        entityData.set(DATA_OWNER, Optional.ofNullable(uuid));
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
        entityData.set(DATA_SITTING, mode == CompanionMode.SIT || mode == CompanionMode.STAY);
        if (mode != CompanionMode.TASK) {
            taskQueue.cancelActive("mode_changed");
        }
        if (mode == CompanionMode.FOLLOW || mode == CompanionMode.WANDER) {
            getNavigation().stop();
        }
    }

    public boolean isSitting() {
        return entityData.get(DATA_SITTING);
    }

    public CompanionInventory getCompanionInventory() {
        return inventory;
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
        tag.putString("Definition", entityData.get(DATA_DEFINITION));
        tag.putString("Mode", entityData.get(DATA_MODE));
        tag.putString("VoiceProfile", voiceProfile);
        tag.putString("SkinPath", getSkinPath());
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
        tag.putString("CustomNameOverride", entityData.get(DATA_CUSTOM_NAME_OVERRIDE));
        tag.put("Inventory", inventory.save(level().registryAccess()));
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Keep existing companions at player-like 20 HP max.
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
        if (tag.contains("Definition")) {
            entityData.set(DATA_DEFINITION, tag.getString("Definition"));
        }
        if (tag.contains("Mode")) {
            entityData.set(DATA_MODE, tag.getString("Mode"));
        }
        voiceProfile = tag.getString("VoiceProfile");
        if (tag.contains("SkinPath")) {
            setSkinPath(tag.getString("SkinPath"));
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
        if (tag.contains("CustomNameOverride")) {
            String override = tag.getString("CustomNameOverride");
            entityData.set(DATA_CUSTOM_NAME_OVERRIDE, override);
            if (!override.isEmpty()) {
                setCustomName(Component.literal(override));
            }
        }
        if (tag.contains("Inventory")) {
            inventory.load(tag.getCompound("Inventory"), level().registryAccess());
        }
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
    }

}
