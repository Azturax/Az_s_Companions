package com.azscompanions.mixin;

import com.azscompanions.event.FabricCompanionRecentActionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerExplosion.class)
public abstract class ExplosionRecentActionMixin {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private Vec3 center;

    @Inject(method = "explode", at = @At("HEAD"))
    private void azscompanions$onExplode(CallbackInfo ci) {
        FabricCompanionRecentActionEvents.onExplosion(level, center.x, center.y, center.z);
    }
}
