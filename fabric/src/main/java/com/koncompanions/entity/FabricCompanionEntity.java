package com.koncompanions.entity;

import com.koncompanions.entity.inventory.FabricCompanionInventory;
import com.koncompanions.menu.FabricCompanionInventoryMenu;
import com.koncompanions.registry.FabricModItems;
import com.koncompanions.task.FabricTaskQueue;
// Radial menu entry points removed (follow-only UX).
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashSet;
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

    private final FabricCompanionInventory inventory = new FabricCompanionInventory();
    private final FabricTaskQueue taskQueue = new FabricTaskQueue(this);
    private final Set<UUID> trusted = new HashSet<>();
    private UUID ownerUuid;
    private BlockPos homePos;
    private BlockPos homeBedPos;
    private String voiceProfile = "kon_soft";
    private boolean konBedGranted;

    public FabricCompanionEntity(EntityType<? extends FabricCompanionEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0d)
                .add(Attributes.MOVEMENT_SPEED, 0.32d)
                .add(Attributes.ATTACK_DAMAGE, 4.0d)
                .add(Attributes.FOLLOW_RANGE, 32.0d)
                .add(Attributes.SCALE, DEFAULT_BODY_SCALE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DEFINITION, FabricCompanionRegistry.KON_ID.toString());
        builder.define(DATA_MODE, FabricCompanionMode.FOLLOW.name());
        builder.define(DATA_NAME, "");
        builder.define(DATA_BODY_SCALE, DEFAULT_BODY_SCALE);
        builder.define(DATA_SKIN_PATH, "");
        builder.define(DATA_SLIM, false);
        builder.define(DATA_GENDER, CompanionGender.FEMALE.getSerializedName());
        builder.define(DATA_BUST, CompanionBodyProportions.DEFAULT_BUST);
        builder.define(DATA_WAIST, CompanionBodyProportions.DEFAULT_WAIST);
        builder.define(DATA_HIPS, CompanionBodyProportions.DEFAULT_HIPS);
        builder.define(DATA_SHOULDERS, CompanionBodyProportions.DEFAULT_SHOULDERS);
        builder.define(DATA_BUST_OFFSET, CompanionBodyProportions.DEFAULT_BUST_OFFSET);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FabricCompanionSleepInBedGoal(this));
        goalSelector.addGoal(2, new FabricFollowOwnerGoal(this));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && level() instanceof ServerLevel) {
            if (getMode() != FabricCompanionMode.FOLLOW) {
                setMode(FabricCompanionMode.FOLLOW);
            }
            taskQueue.cancelActive();
            Player owner = getOwner();
            if (owner != null && distanceTo(owner) > 48) {
                teleportTo(owner.getX(), owner.getY(), owner.getZ());
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer) || !isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            openInventory((ServerPlayer) player);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
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

    public void sayHello() {
        sayOwnerChatLine("dialogue.koncompanions.hello");
    }

    public void sayBye() {
        sayOwnerChatLine("dialogue.koncompanions.bye");
    }

    private void sayOwnerChatLine(String langKey) {
        if (level().isClientSide) {
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
        ownerUuid = player.getUUID();
        trusted.add(player.getUUID());
    }

    public boolean isOwnedBy(Player player) {
        return ownerUuid != null && ownerUuid.equals(player.getUUID());
    }

    public Player getOwner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public FabricCompanionMode getMode() {
        return FabricCompanionMode.byName(entityData.get(DATA_MODE));
    }

    public void setMode(FabricCompanionMode mode) {
        entityData.set(DATA_MODE, mode.name());
    }

    public FabricCompanionInventory getCompanionInventory() {
        return inventory;
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
        player.displayClientMessage(Component.translatable("message.koncompanions.kon_bed_granted"), true);
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
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
        tag.putString("Definition", entityData.get(DATA_DEFINITION));
        tag.putString("Mode", entityData.get(DATA_MODE));
        tag.putString("SkinPath", getSkinPath());
        tag.putString("CustomNameOverride", entityData.get(DATA_NAME));
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
        if (homePos != null) {
            tag.putLong("HomePos", homePos.asLong());
        }
        if (homeBedPos != null) {
            tag.putLong("HomeBedPos", homeBedPos.asLong());
        }
        tag.put("Inventory", inventory.createTag(level().registryAccess()));
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
            ownerUuid = tag.getUUID("Owner");
        }
        if (tag.contains("Definition")) {
            entityData.set(DATA_DEFINITION, tag.getString("Definition"));
        }
        if (tag.contains("Mode")) {
            entityData.set(DATA_MODE, tag.getString("Mode"));
        }
        if (tag.contains("SkinPath")) {
            setSkinPath(tag.getString("SkinPath"));
        }
        konBedGranted = tag.contains("KonBedGranted") && tag.getBoolean("KonBedGranted");
        if (tag.contains("CustomNameOverride") && !tag.getString("CustomNameOverride").isEmpty()) {
            // Load name without re-triggering Kon special grants.
            String override = tag.getString("CustomNameOverride");
            entityData.set(DATA_NAME, override);
            setCustomName(Component.literal(override));
        }
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
    }
}
