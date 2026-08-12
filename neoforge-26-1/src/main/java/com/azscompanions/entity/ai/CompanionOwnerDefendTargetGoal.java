package com.azscompanions.entity.ai;

import com.azscompanions.config.ServerConfig;
import com.azscompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * Defend the owner against real living attackers only.
 * Ignores environmental damage (fall, cactus, lava, drowning, etc.) because those never
 * set {@link LivingEntity#getLastHurtByMob()} to a living entity.
 */
public final class CompanionOwnerDefendTargetGoal extends TargetGoal {
    private static final int HURT_MEMORY_TICKS = 200;
    private static final double SCAN_RANGE = 48.0d;

    private final CompanionEntity companion;
    private LivingEntity ownerAttacker;
    private int lastOwnerHurtTimestamp;

    public CompanionOwnerDefendTargetGoal(CompanionEntity companion) {
        super(companion, false, true);
        this.companion = companion;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!ServerConfig.ALLOW_COMBAT.get() || !companion.hasPermission("combat")) {
            return false;
        }
        if (companion.isSitting() || companion.isSleeping()) {
            return false;
        }
        Player owner = companion.getOwner();
        if (owner == null || !owner.isAlive()) {
            return false;
        }

        LivingEntity hurtBy = owner.getLastHurtByMob();
        int hurtStamp = owner.getLastHurtByMobTimestamp();
        if (hurtBy != null
                && hurtStamp != lastOwnerHurtTimestamp
                && owner.tickCount - hurtStamp <= HURT_MEMORY_TICKS
                && isValidThreat(hurtBy, owner)) {
            ownerAttacker = hurtBy;
            return true;
        }

        LivingEntity targetingOwner = findMobTargetingOwner(owner);
        if (targetingOwner != null) {
            ownerAttacker = targetingOwner;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        Player owner = companion.getOwner();
        if (owner != null) {
            lastOwnerHurtTimestamp = owner.getLastHurtByMobTimestamp();
        }
        mob.setTarget(ownerAttacker);
        super.start();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = companion.getTarget();
        Player owner = companion.getOwner();
        if (target == null || !target.isAlive() || owner == null) {
            return false;
        }
        if (!isValidThreat(target, owner)) {
            return false;
        }
        // Keep fighting while the mob still targets the owner, or recent owner-hurt memory.
        if (target instanceof Mob mob && mob.getTarget() == owner) {
            return true;
        }
        return owner.getLastHurtByMob() == target
                && owner.tickCount - owner.getLastHurtByMobTimestamp() <= HURT_MEMORY_TICKS;
    }

    @Override
    public void stop() {
        ownerAttacker = null;
        companion.setTarget(null);
        super.stop();
    }

    private LivingEntity findMobTargetingOwner(Player owner) {
        AABB box = companion.getBoundingBox().inflate(SCAN_RANGE);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Mob mob : companion.level().getEntitiesOfClass(Mob.class, box)) {
            if (mob.getTarget() != owner) {
                continue;
            }
            if (!isValidThreat(mob, owner)) {
                continue;
            }
            double dist = companion.distanceToSqr(mob);
            if (dist < bestDist) {
                bestDist = dist;
                best = mob;
            }
        }
        return best;
    }

    private boolean isValidThreat(LivingEntity threat, Player owner) {
        if (threat == null || !threat.isAlive() || threat == companion || threat == owner) {
            return false;
        }
        // Must be a real living combatant — never block/environment "attackers".
        if (!(threat instanceof Mob) && !(threat instanceof Player)) {
            return false;
        }
        if (!(companion.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || !TargetingConditions.forCombat().test(serverLevel, companion, threat)) {
            return false;
        }
        return companion.canAttackTarget(threat);
    }
}
