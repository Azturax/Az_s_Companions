package com.azscompanions.item;

import com.azscompanions.entity.FabricCompanionDimensionTravelSupport;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionRecruitment;
import com.azscompanions.entity.FabricCompanionRegistry;
import com.azscompanions.registry.FabricModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class FabricCompanionCharmItem extends Item {
    public FabricCompanionCharmItem(Properties properties) {
        super(properties);
    }

    /** True if the stack is a Companion Charm (companions must never hold one). */
    public static boolean isCharm(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == FabricModItems.COMPANION_CHARM;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            toggleOrRecruit(serverPlayer, stack);
        }
        return InteractionResult.SUCCESS;
    }

    private static void toggleOrRecruit(ServerPlayer player, ItemStack stack) {
        UUID bound = FabricCharmData.getBoundUuid(stack);
        if (bound == null) {
            FabricCompanionEntity created = FabricCompanionRecruitment.recruitEntity(player, FabricCompanionRegistry.KON_ID.toString());
            if (created != null) {
                FabricCharmData.bind(stack, created.getUUID());
                created.sayHello();
                player.displayClientMessage(Component.translatable("message.azscompanions.charm_bound"), true);
                com.azscompanions.ai.FabricCompanionPersonaOnboarding.offerIfNeeded(player, created);
            }
            return;
        }
        FabricCompanionEntity living = FabricCompanionRecruitment.findOwned(player, bound);
        if (living != null) {
            living.sayBye();
            living.despawnChildCompanions();
            var tag = new net.minecraft.nbt.CompoundTag();
            living.saveWithoutId(tag);
            FabricCharmData.storeCompanion(stack, tag, bound);
            living.discard();
            player.displayClientMessage(Component.translatable("message.azscompanions.charm_despawned"), true);
            return;
        }
        if (FabricCharmData.hasStoredCompanion(stack)) {
            var stored = FabricCharmData.peekStoredCompanion(stack);
            if (stored != null) {
                FabricCompanionEntity spawned = FabricCompanionRecruitment.spawnFromStored(player, stored.copy(), bound);
                if (spawned != null) {
                    FabricCharmData.clearStoredCompanion(stack);
                    spawned.sayHello();
                    player.displayClientMessage(Component.translatable("message.azscompanions.charm_summoned"), true);
                }
            }
            return;
        }

        // Bound but missing: restore world identity snapshot (same UUID) — not a fresh create.
        var server = player.getServer();
        if (server != null) {
            var identity = com.azscompanions.world.FabricCompanionIdentityStore.get(server).peekIdentity(bound);
            if (identity != null) {
                FabricCompanionEntity restored =
                        FabricCompanionRecruitment.spawnFromStored(player, identity.copy(), bound);
                if (restored != null) {
                    FabricCharmData.clearLogoutParked(stack);
                    restored.sayHello();
                    player.displayClientMessage(Component.translatable("message.azscompanions.charm_summoned"), true);
                    FabricCompanionDimensionTravelSupport.rememberIdentity(player, restored);
                    return;
                }
            }
        }

        FabricCompanionEntity created = FabricCompanionRecruitment.recruitEntity(player, FabricCompanionRegistry.KON_ID.toString());
        if (created != null) {
            FabricCharmData.bind(stack, created.getUUID());
            created.sayHello();
            player.displayClientMessage(Component.translatable("message.azscompanions.charm_bound"), true);
            com.azscompanions.ai.FabricCompanionPersonaOnboarding.offerIfNeeded(player, created);
            FabricCompanionDimensionTravelSupport.rememberIdentity(player, created);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (entity instanceof Player player) {
            enforceSingleCharm(player);
        }
    }

    public static void enforceSingleCharm(Player player) {
        boolean kept = false;
        boolean dropped = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot.getItem() != FabricModItems.COMPANION_CHARM) {
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
            player.displayClientMessage(Component.translatable("message.azscompanions.charm_only_one"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.azscompanions.companion_charm.desc"));
        if (FabricCharmData.isBound(stack)) {
            tooltip.accept(Component.translatable(
                    FabricCharmData.hasStoredCompanion(stack)
                            ? "item.azscompanions.companion_charm.bound_stored"
                            : "item.azscompanions.companion_charm.bound_active"));
        } else {
            tooltip.accept(Component.translatable("item.azscompanions.companion_charm.unbound"));
        }
    }
}
