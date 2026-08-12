package com.azscompanions.entity.ai;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMobBehaviorSupport;
import com.azscompanions.entity.CompanionMobBehaviorSupport.WanderInteractKind;
import com.azscompanions.entity.CompanionMode;
import com.azscompanions.util.ProtectionHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Casual wander-mode play: walk around / sneak near / push / light-punch nearby mobs.
 * Does not run in Follow/Stay/Sit and never sets a combat target.
 */
public final class CompanionWanderMobInteractGoal extends Goal {
    private final CompanionEntity companion;
    private LivingEntity target;
    private WanderInteractKind kind;
    private int cooldown;
    private int ticksLeft;
    private double angle;
    private boolean didNudge;

    public CompanionWanderMobInteractGoal(CompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!CompanionMobBehaviorSupport.canStartWanderInteract(
                companion.getMode() == CompanionMode.WANDER,
                companion.isSitting(),
                companion.isSleeping(),
                companion.getTarget() != null && companion.getTarget().isAlive(),
                0)) {
            return false;
        }
        if (companion.getRandom().nextInt(CompanionMobBehaviorSupport.INTERACT_START_CHANCE) != 0) {
            return false;
        }
        target = findTarget();
        if (target == null) {
            return false;
        }
        kind = CompanionMobBehaviorSupport.pickKind(companion.getRandom().nextInt(100));
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!CompanionMobBehaviorSupport.canStartWanderInteract(
                companion.getMode() == CompanionMode.WANDER,
                companion.isSitting(),
                companion.isSleeping(),
                companion.getTarget() != null && companion.getTarget().isAlive(),
                0)) {
            return false;
        }
        if (target == null || !target.isAlive() || ticksLeft <= 0) {
            return false;
        }
        return companion.distanceToSqr(target) <= CompanionMobBehaviorSupport.INTERACT_RANGE_SQR * 1.5d;
    }

    @Override
    public void start() {
        ticksLeft = CompanionMobBehaviorSupport.durationTicks(companion.getRandom().nextInt(10_000));
        angle = companion.getRandom().nextDouble() * Math.PI * 2.0d;
        didNudge = false;
        companion.setShiftKeyDown(kind == WanderInteractKind.SNEAK);
    }

    @Override
    public void stop() {
        companion.setShiftKeyDown(false);
        companion.getNavigation().stop();
        target = null;
        kind = null;
        ticksLeft = 0;
        cooldown = CompanionMobBehaviorSupport.cooldownTicks(companion.getRandom().nextInt(10_000));
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        ticksLeft--;
        companion.getLookControl().setLookAt(target, 30.0f, companion.getMaxHeadXRot());

        if (kind == WanderInteractKind.SNEAK) {
            companion.setShiftKeyDown(true);
        }

        switch (kind) {
            case CIRCLE, SNEAK -> tickCircle();
            case PUSH -> tickPush();
            case PUNCH -> tickPunch();
        }
    }

    private void tickCircle() {
        angle += 0.12d;
        double[] xz = CompanionMobBehaviorSupport.circlePoint(
                target.getX(), target.getZ(), angle, CompanionMobBehaviorSupport.CIRCLE_RADIUS);
        companion.getNavigation().moveTo(xz[0], target.getY(), xz[1], CompanionMobBehaviorSupport.APPROACH_SPEED);
    }

    private void tickPush() {
        if (companion.distanceToSqr(target) > 2.25d) {
            companion.getNavigation().moveTo(target, CompanionMobBehaviorSupport.APPROACH_SPEED);
            return;
        }
        companion.getNavigation().stop();
        if (!didNudge && ticksLeft < CompanionMobBehaviorSupport.INTERACT_DURATION_MIN) {
            applyKnockback(CompanionMobBehaviorSupport.PUSH_STRENGTH);
            didNudge = true;
        }
    }

    private void tickPunch() {
        if (companion.distanceToSqr(target) > 2.25d) {
            companion.getNavigation().moveTo(target, CompanionMobBehaviorSupport.APPROACH_SPEED + 0.15d);
            return;
        }
        companion.getNavigation().stop();
        if (!didNudge) {
            companion.swing(InteractionHand.MAIN_HAND);
            applyKnockback(CompanionMobBehaviorSupport.PUNCH_KNOCKBACK);
            if (CompanionMobBehaviorSupport.punchDealsDamage(ServerConfig.ALLOW_COMBAT.get())) {
                target.hurt(companion.damageSources().mobAttack(companion), CompanionMobBehaviorSupport.PUNCH_DAMAGE);
            }
            didNudge = true;
            ticksLeft = Math.min(ticksLeft, 12);
        }
    }

    private void applyKnockback(double strength) {
        double[] dir = CompanionMobBehaviorSupport.knockbackDir(
                companion.getX(), companion.getZ(), target.getX(), target.getZ());
        target.knockback(
                strength,
                -dir[0],
                -dir[1],
                companion.damageSources().mobAttack(companion),
                0.0f);
    }

    private LivingEntity findTarget() {
        AABB box = companion.getBoundingBox().inflate(CompanionMobBehaviorSupport.INTERACT_RANGE);
        List<LivingEntity> found = companion.level().getEntitiesOfClass(
                LivingEntity.class, box, this::isValidTarget);
        if (found.isEmpty()) {
            return null;
        }
        return found.get(companion.getRandom().nextInt(found.size()));
    }

    private boolean isValidTarget(LivingEntity living) {
        if (ProtectionHelper.isProtectedEntity(living)) {
            return false;
        }
        boolean ownerPet = false;
        if (living instanceof OwnableEntity ownable) {
            UUID petOwner = null;
            var ref = ownable.getOwnerReference();
            if (ref != null) {
                petOwner = ref.getUUID();
            }
            UUID owner = companion.getOwnerUuid();
            if (petOwner != null && owner != null && petOwner.equals(owner)) {
                ownerPet = true;
            }
        }
        boolean bossLike = living instanceof EnderDragon
                || living instanceof WitherBoss
                || living instanceof Warden
                || living instanceof ElderGuardian;
        if (!CompanionMobBehaviorSupport.isValidInteractTarget(
                living.isAlive(),
                living instanceof Player,
                living instanceof CompanionEntity,
                ownerPet,
                bossLike)) {
            return false;
        }
        if (!(living instanceof PathfinderMob) && !living.getType().getCategory().isFriendly()) {
            return false;
        }
        return companion.distanceToSqr(living) <= CompanionMobBehaviorSupport.INTERACT_RANGE_SQR;
    }
}
