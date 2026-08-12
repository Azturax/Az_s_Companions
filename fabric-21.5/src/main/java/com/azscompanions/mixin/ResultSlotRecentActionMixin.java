package com.azscompanions.mixin;

import com.azscompanions.event.FabricCompanionRecentActionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class ResultSlotRecentActionMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void azscompanions$onCraftTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            FabricCompanionRecentActionEvents.onPlayerCrafted(serverPlayer, stack);
        }
    }
}
