package com.azscompanions.mixin;

import com.azscompanions.event.FabricCompanionRecentActionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionRecentActionMixin {
    @Shadow @Final private Level level;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void azscompanions$onFinalizeExplosion(boolean spawnParticles, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            FabricCompanionRecentActionEvents.onExplosion(serverLevel, x, y, z);
        }
    }
}
