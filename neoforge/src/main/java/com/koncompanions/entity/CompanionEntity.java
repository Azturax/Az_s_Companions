package com.koncompanions.entity;

import com.koncompanions.config.CommonConfig;
import com.koncompanions.config.ServerConfig;
import com.koncompanions.dialogue.CompanionChatMatcher;
import com.koncompanions.entity.ai.CompanionFollowGoal;
import com.koncompanions.entity.ai.CompanionLookAtOwnerGoal;
import com.koncompanions.entity.ai.CompanionSleepInBedGoal;
import com.koncompanions.entity.inventory.CompanionInventory;
import com.koncompanions.menu.CompanionInventoryMenu;
import com.koncompanions.menu.CompanionManagementMenu;
import com.koncompanions.menu.RadialCommandMenu;
import com.koncompanions.network.packet.OpenCompanionCreatorPacket;
import com.koncompanions.task.TaskQueue;
import com.koncompanions.util.ProtectionHelper;
import com.koncompanions.voice.DialogueCategory;
import com.koncompanions.voice.VoiceService;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
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

    private final CompanionInventory inventory = new CompanionInventory();
    private final TaskQueue taskQueue = new TaskQueue(this);
    private final Set<UUID> trustedPlayers = new HashSet<>();
    private final Set<String> permissions = new HashSet<>();

    @Nullable
    private UUID ownerUuid;
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
    /** Game time of last chat reply (cooldown). */
    private long lastChatReplyGameTime = Long.MIN_VALUE;
    private static final int CHAT_REPLY_COOLDOWN_TICKS = 30; // 1.5s
    // skinPath / bodyScale / slimArms live in synched entity data

    public CompanionEntity(EntityType<? extends CompanionEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation ground) {
            ground.setCanOpenDoors(true);
            ground.setCanFloat(true);
        }
        this.setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0d)
                .add(Attributes.MOVEMENT_SPEED, 0.32d)
                .add(Attributes.ATTACK_DAMAGE, 4.0d)
                .add(Attributes.FOLLOW_RANGE, 32.0d)
                .add(Attributes.ARMOR, 2.0d)
                .add(Attributes.SCALE, DEFAULT_BODY_SCALE);
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
        builder.define(DATA_BUST, CompanionBodyProportions.DEFAULT_BUST);
        builder.define(DATA_WAIST, CompanionBodyProportions.DEFAULT_WAIST);
        builder.define(DATA_HIPS, CompanionBodyProportions.DEFAULT_HIPS);
        builder.define(DATA_SHOULDERS, CompanionBodyProportions.DEFAULT_SHOULDERS);
        builder.define(DATA_BUST_OFFSET, CompanionBodyProportions.DEFAULT_BUST_OFFSET);
    }

    @Override
    protected void registerGoals() {
        // Day: follow owner. Night: sleep in nearest Kon bed (home).
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CompanionSleepInBedGoal(this));
        goalSelector.addGoal(2, new CompanionFollowGoal(this));
        goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        goalSelector.addGoal(4, new CompanionLookAtOwnerGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && level() instanceof ServerLevel) {
            // Follow-only: keep mode locked and clear any leftover task queue state.
            if (getMode() != CompanionMode.FOLLOW || isSitting()) {
                setMode(CompanionMode.FOLLOW);
            }
            if (taskQueue.getActive() != null || !taskQueue.queued().isEmpty()) {
                taskQueue.clear();
            }
            tickStuckRecovery();
            tickSurvival();
        }
    }

    private void tickStuckRecovery() {
        if (position().distanceToSqr(lastPos) < 0.01d && !getNavigation().isDone()) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPos = position();
        }
        if (stuckTicks > CommonConfig.PATH_STUCK_TIMEOUT_TICKS.get() && CommonConfig.TELEPORT_WHEN_STUCK.get()) {
            Player owner = getOwner();
            if (owner != null && distanceTo(owner) > 8) {
                safeTeleportNear(owner.blockPosition());
                stuckTicks = 0;
                speak(DialogueCategory.TASK_PROGRESS);
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
        for (int i = 0; i < 8; i++) {
            BlockPos candidate = target.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
            if (level().getBlockState(candidate).isAir() && level().getBlockState(candidate.below()).isSolid()) {
                teleportTo(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
                return;
            }
        }
        teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }
        if (!isOwnedBy(player) && !isTrusted(player)) {
            return InteractionResult.PASS;
        }

        // Shift + right-click opens Customize. Normal right-click does nothing.
        if (player.isShiftKeyDown()) {
            PacketDistributor.sendToPlayer((ServerPlayer) player, new OpenCompanionCreatorPacket(getId()));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    public void openRadial(ServerPlayer player) {
        player.openMenu(new RadialCommandMenu.Provider(this), buf -> buf.writeVarInt(getId()));
    }

    public void openManagement(ServerPlayer player) {
        player.openMenu(new CompanionManagementMenu.Provider(this), buf -> buf.writeVarInt(getId()));
    }

    public void openInventory(ServerPlayer player) {
        player.openMenu(new CompanionInventoryMenu.Provider(this), buf -> buf.writeVarInt(getId()));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && (isOwnedBy(player) || isTrusted(player))) {
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
            if (petOwner != null && (petOwner.equals(ownerUuid) || trustedPlayers.contains(petOwner))) {
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

    /**
     * Reply to owner chat if a keyword matches, or if the message addresses Kon by name.
     * @return true if a reply was spoken
     */
    public boolean tryReplyToChat(String message) {
        if (level().isClientSide) {
            return false;
        }
        long now = level().getGameTime();
        if (now - lastChatReplyGameTime < CHAT_REPLY_COOLDOWN_TICKS) {
            return false;
        }
        var key = CompanionChatMatcher.match(message, getChatDisplayName());
        if (key.isEmpty()) {
            return false;
        }
        String line = Component.translatable(key.get()).getString();
        lastChatReplyGameTime = now;
        VoiceService.get().speak(this, DialogueCategory.IDLE, line);
        return true;
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
        return ownerUuid != null && ownerUuid.equals(player.getUUID());
    }

    public boolean isTrusted(Player player) {
        return trustedPlayers.contains(player.getUUID());
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getUUID();
        trustedPlayers.add(player.getUUID());
    }

    @Nullable
    public Player getOwner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
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
        entityData.set(DATA_SITTING, mode == CompanionMode.SIT);
        if (mode != CompanionMode.TASK) {
            taskQueue.cancelActive("mode_changed");
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
        entityData.set(DATA_CUSTOM_NAME_OVERRIDE, name);
        setCustomName(Component.literal(name));
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
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putString("Definition", entityData.get(DATA_DEFINITION));
        tag.putString("Mode", entityData.get(DATA_MODE));
        tag.putString("VoiceProfile", voiceProfile);
        tag.putString("SkinPath", getSkinPath());
        tag.putBoolean("SlimArms", isSlimArms());
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
            ownerUuid = tag.getUUID("Owner");
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
