package com.azscompanions.item;

import com.azscompanions.entity.FlyingNimbusEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Calls / dismisses Flying Nimbus (Jindujun). Loot: Trail Ruins archaeology, taiga, 2%.
 */
public final class JindujunWhistleItem extends Item {
    public JindujunWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            toggle(serverPlayer, serverLevel);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void toggle(ServerPlayer player, ServerLevel level) {
        if (player.getVehicle() instanceof FlyingNimbusEntity riding && riding.isOwnedBy(player)) {
            player.stopRiding();
            riding.discard();
            player.displayClientMessage(Component.translatable("message.azscompanions.jindujun_dismissed"), true);
            return;
        }

        FlyingNimbusEntity existing = findOwned(level, player);
        if (existing != null) {
            existing.moveTo(player.getX(), player.getY() + 0.15d, player.getZ(), player.getYRot(), 0.0f);
            player.startRiding(existing, true);
            player.displayClientMessage(Component.translatable("message.azscompanions.jindujun_recalled"), true);
            return;
        }

        FlyingNimbusEntity cloud = FlyingNimbusEntity.createFor(level, player);
        if (cloud == null) {
            return;
        }
        level.addFreshEntity(cloud);
        player.startRiding(cloud, true);
        player.displayClientMessage(Component.translatable("message.azscompanions.jindujun_summoned"), true);
    }

    private static FlyingNimbusEntity findOwned(ServerLevel level, Player player) {
        AABB box = player.getBoundingBox().inflate(64.0d);
        List<FlyingNimbusEntity> list = level.getEntitiesOfClass(
                FlyingNimbusEntity.class,
                box,
                n -> n.isAlive() && n.isOwnedBy(player));
        return list.isEmpty() ? null : list.getFirst();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.azscompanions.jindujun_whistle.desc"));
    }
}
