package com.azscompanions.item;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionRecruitment;
import com.azscompanions.entity.CompanionRegistry;
import com.azscompanions.perk.MisterWigglySidekick;
import com.azscompanions.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

public final class CompanionCharmItem extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();

    public CompanionCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            toggleOrRecruit(serverPlayer, stack);
        }
        return InteractionResult.SUCCESS;
    }

    private static void toggleOrRecruit(ServerPlayer player, ItemStack stack) {
        UUID bound = CharmData.getBoundUuid(stack);
        if (bound == null) {
            CompanionEntity created = CompanionRecruitment.recruit(player, CompanionRegistry.KON_ID.toString());
            if (created != null) {
                CharmData.bind(stack, created.getUUID());
                MisterWigglySidekick.ensureFor(created);
                created.sayHello();
                player.sendOverlayMessage(Component.translatable("message.azscompanions.charm_bound"));
            }
            return;
        }

        CompanionEntity living = CompanionRecruitment.findOwned(player, bound);
        if (living != null) {
            living.sayBye();
            MisterWigglySidekick.despawnFor(living);
            living.despawnChildCompanions();
            CompoundTag entityTag;
            try (ProblemReporter.ScopedCollector reporter =
                         new ProblemReporter.ScopedCollector(living.problemPath(), LOGGER)) {
                TagValueOutput output = TagValueOutput.createWithContext(reporter, living.registryAccess());
                living.saveWithoutId(output);
                entityTag = output.buildResult();
            }
            CharmData.storeCompanion(stack, entityTag, bound);
            living.discard();
            player.sendOverlayMessage(Component.translatable("message.azscompanions.charm_despawned"));
            return;
        }

        if (CharmData.hasStoredCompanion(stack)) {
            CompoundTag stored = CharmData.peekStoredCompanion(stack);
            if (stored != null) {
                CompanionEntity spawned = CompanionRecruitment.spawnFromStored(player, stored.copy(), bound);
                if (spawned != null) {
                    CharmData.clearStoredCompanion(stack);
                    MisterWigglySidekick.ensureFor(spawned);
                    spawned.sayHello();
                    player.sendOverlayMessage(Component.translatable("message.azscompanions.charm_summoned"));
                }
            }
            return;
        }

        CompanionEntity created = CompanionRecruitment.recruit(player, CompanionRegistry.KON_ID.toString());
        if (created != null) {
            CharmData.bind(stack, created.getUUID());
            MisterWigglySidekick.ensureFor(created);
            created.sayHello();
            player.sendOverlayMessage(Component.translatable("message.azscompanions.charm_bound"));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        enforceSingleCharm(player);
    }

    public static void enforceSingleCharm(Player player) {
        boolean kept = false;
        boolean dropped = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (!slot.is(ModItems.COMPANION_CHARM.get())) {
                continue;
            }
            if (!kept) {
                kept = true;
                continue;
            }
            ItemStack drop = slot.copy();
            player.getInventory().setItem(i, ItemStack.EMPTY);
            ItemEntity entity = player.drop(drop, false);
            if (entity != null) {
                entity.setPickUpDelay(40);
            }
            dropped = true;
        }
        if (dropped) {
            player.sendOverlayMessage(Component.translatable("message.azscompanions.charm_only_one"));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.azscompanions.companion_charm.desc"));
        if (CharmData.isBound(stack)) {
            tooltip.add(Component.translatable(
                    CharmData.hasStoredCompanion(stack)
                            ? "item.azscompanions.companion_charm.bound_stored"
                            : "item.azscompanions.companion_charm.bound_active"));
        } else {
            tooltip.add(Component.translatable("item.azscompanions.companion_charm.unbound"));
        }
    }
}
