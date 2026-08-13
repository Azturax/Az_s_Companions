package com.azscompanions.mixin.cci;

import com.azscompanions.AzsCompanionsConstants;
import com.azscompanions.compat.cci.FabricCciBridge;
import me.ichun.mods.cci.common.config.outcome.IMCOutcome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * iChunUtil's Fabric {@code sendIMCMessage} always returns false (no Forge IMC).
 * When CCI fires an IMCOutcome for our mod id, forward the subject/message into our bridge
 * so NeoForge-style IMC configs also work on Fabric CCI.
 */
@Mixin(value = IMCOutcome.class, remap = false)
public abstract class IMCOutcomeMixin {
    @Inject(method = "triggerClientSide", at = @At("RETURN"), cancellable = true, remap = false)
    private void azscompanions$fabricImcBridge(String message, String modId, String subject,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        if (modId == null || !AzsCompanionsConstants.MOD_ID.equals(modId)) {
            return;
        }
        FabricCciBridge.dispatch(subject, message);
        cir.setReturnValue(true);
    }
}
