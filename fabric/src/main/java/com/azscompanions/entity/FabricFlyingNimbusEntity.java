package com.azscompanions.entity;

import com.azscompanions.registry.FabricModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Rideable Flying Nimbus (Jindujun). Player-controlled while mounted.
 */
public final class FabricFlyingNimbusEntity extends Mob implements PlayerRideable {
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(FabricFlyingNimbusEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public FabricFlyingNimbusEntity(EntityType<? extends FabricFlyingNimbusEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0d)
                .add(Attributes.MOVEMENT_SPEED, JindujunSupport.FLY_SPEED)
                .add(Attributes.FOLLOW_RANGE, 16.0d);
    }

    public static FabricFlyingNimbusEntity createFor(ServerLevel level, Player owner) {
        FabricFlyingNimbusEntity cloud = FabricModEntities.FLYING_NIMBUS.create(level);
        if (cloud == null) {
            return null;
        }
        cloud.setOwnerUUID(owner.getUUID());
        cloud.moveTo(owner.getX(), owner.getY() + 0.2d, owner.getZ(), owner.getYRot(), 0.0f);
        return cloud;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        entityData.set(DATA_OWNER, Optional.ofNullable(uuid));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return entityData.get(DATA_OWNER).orElse(null);
    }

    public boolean isOwnedBy(Player player) {
        UUID owner = getOwnerUUID();
        return player != null && owner != null && owner.equals(player.getUUID());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    public boolean shouldRiderSit() {
        return true;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity first = getFirstPassenger();
        return first instanceof LivingEntity living ? living : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        callback.accept(passenger, getX(), getY() + JindujunSupport.RIDER_Y_OFFSET, getZ());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && !player.isShiftKeyDown() && canAddPassenger(player)) {
            if (getOwnerUUID() == null) {
                setOwnerUUID(player.getUUID());
            }
            if (isOwnedBy(player) || getOwnerUUID() == null) {
                player.startRiding(this);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isAlive() && isVehicle() && getControllingPassenger() instanceof Player player) {
            setYRot(player.getYRot());
            yRotO = getYRot();
            setXRot(player.getXRot() * 0.35f);
            setRot(getYRot(), getXRot());
            yBodyRot = getYRot();
            yHeadRot = getYRot();

            float strafe = player.xxa * 0.5f;
            float forward = player.zza;
            double vertical = 0.0d;
            // LivingEntity.jumping is protected — climb via look-up (and pitch-forward below).
            if (player.getXRot() < -25.0f) {
                vertical += JindujunSupport.VERTICAL_SPEED;
            }
            if (player.isShiftKeyDown()) {
                vertical -= JindujunSupport.VERTICAL_SPEED;
            }
            if (Math.abs(forward) > 0.01f) {
                vertical += -player.getXRot() / 90.0d * JindujunSupport.VERTICAL_SPEED * 0.65d * forward;
            }

            float yawRad = getYRot() * ((float) Math.PI / 180.0f);
            double sin = Mth.sin(yawRad);
            double cos = Mth.cos(yawRad);
            double speed = JindujunSupport.FLY_SPEED;
            double vx = (strafe * cos - forward * sin) * speed;
            double vz = (forward * cos + strafe * sin) * speed;
            setDeltaMovement(vx, vertical, vz);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(JindujunSupport.IDLE_DAMP));
        } else {
            setDeltaMovement(getDeltaMovement().multiply(0.7d, 0.7d, 0.7d));
            setNoGravity(true);
            super.travel(travelVector);
        }
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
        fallDistance = 0.0f;
        if (!isVehicle() && level().isClientSide) {
            setPos(getX(), getY() + Math.sin(tickCount * 0.08d) * 0.002d, getZ());
        }
        if (level().isClientSide) {
            var delta = getDeltaMovement();
            JindujunSupport.spawnEnchantStream(
                    isVehicle(),
                    delta.x,
                    delta.y,
                    delta.z,
                    tickCount,
                    getX(),
                    getY(),
                    getZ(),
                    getYRot(),
                    (x, y, z, vx, vy, vz) ->
                            level().addParticle(ParticleTypes.ENCHANT, x, y, z, vx, vy, vz));
        }
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = getOwnerUUID();
        if (owner != null) {
            tag.putUUID(JindujunSupport.NBT_OWNER, owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(JindujunSupport.NBT_OWNER)) {
            setOwnerUUID(tag.getUUID(JindujunSupport.NBT_OWNER));
        }
    }

    @Override
    protected void registerGoals() {
    }
}
