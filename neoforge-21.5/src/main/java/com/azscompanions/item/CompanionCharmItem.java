package com.azscompanions.item;

import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionRecruitment;
import com.azscompanions.entity.CompanionRegistry;
import com.azscompanions.perk.MisterWigglySidekick;
import com.azscompanions.registry.ModItems;
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

/**
 * Loot-only Companion Charm. Unbound: first use recruits Kon and binds.
 * Bound: toggles appear (summon from stored NBT) / disappear (store + despawn).
 * Players may hold only one charm; extras are dropped.
 */
public final class CompanionCharmItem extends Item {
    public CompanionCharmItem(Properties properties) {
        super(properties);
    }

    /** True if the stack is a Companion Charm (companions must never hold one). */
    public static boolean isCharm(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(ModItems.COMPANION_CHARM.get());
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
        UUID bound = CharmData.getBoundUuid(stack);

        if (bound == null) {
            CompanionEntity created = CompanionRecruitment.recruit(player, CompanionRegistry.KON_ID.toString());
            if (created != null) {
                CharmData.bind(stack, created.getUUID());
                MisterWigglySidekick.ensureFor(created);
                created.sayHello();
                player.displayClientMessage(Component.translatable("message.azscompanions.charm_bound"), true);
                com.azscompanions.ai.CompanionPersonaOnboarding.offerIfNeeded(player, created);
            }
            return;
        }

        CompanionEntity living = CompanionRecruitment.findOwned(player, bound);
        if (living != null) {
            living.sayBye();
            MisterWigglySidekick.despawnFor(living);
            living.despawnChildCompanions();
            var entityTag = new net.minecraft.nbt.CompoundTag();
            living.saveWithoutId(entityTag);
            CharmData.storeCompanion(stack, entityTag, bound);
            living.discard();
            player.displayClientMessage(Component.translatable("message.azscompanions.charm_despawned"), true);
            return;
        }

        if (CharmData.hasStoredCompanion(stack)) {
            var stored = CharmData.peekStoredCompanion(stack);
            if (stored != null) {
                CompanionEntity spawned = CompanionRecruitment.spawnFromStored(player, stored.copy(), bound);
                if (spawned != null) {
                    CharmData.clearStoredCompanion(stack);
                    MisterWigglySidekick.ensureFor(spawned);
                    spawned.sayHello();
                    player.displayClientMessage(Component.translatable("message.azscompanions.charm_summoned"), true);
                }
            }
            return;
        }

        // Bound but missing (unloaded / left behind before dimension travel): restore world identity first.
        // Never treat this as a fresh-world create — do not re-open persona onboarding.
        var server = player.getServer();
        if (server != null) {
            var identity = com.azscompanions.world.CompanionIdentityStore.get(server).peekIdentity(bound);
            if (identity != null) {
                var payload = identity.copy();
                CompanionEntity restored = CompanionRecruitment.spawnFromStored(player, payload, bound);
                if (restored != null) {
                    CharmData.clearLogoutParked(stack);
                    MisterWigglySidekick.ensureFor(restored);
                    restored.sayHello();
                    player.displayClientMessage(Component.translatable("message.azscompanions.charm_summoned"), true);
                    com.azscompanions.entity.CompanionDimensionTravelSupport.rememberIdentity(player, restored);
                    return;
                }
            }
        }

        // Last resort: recruit replacement and rebind (new companion → persona onboarding OK).
        CompanionEntity created = CompanionRecruitment.recruit(player, CompanionRegistry.KON_ID.toString());
        if (created != null) {
            CharmData.bind(stack, created.getUUID());
            MisterWigglySidekick.ensureFor(created);
            created.sayHello();
            player.displayClientMessage(Component.translatable("message.azscompanions.charm_bound"), true);
            com.azscompanions.ai.CompanionPersonaOnboarding.offerIfNeeded(player, created);
            com.azscompanions.entity.CompanionDimensionTravelSupport.rememberIdentity(player, created);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player)) {
            return;
        }
        enforceSingleCharm(player);
    }

    /** Drop every charm after the first found in the player inventory. */
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
            player.displayClientMessage(Component.translatable("message.azscompanions.charm_only_one"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.azscompanions.companion_charm.desc"));
        if (CharmData.isBound(stack)) {
            tooltip.accept(Component.translatable(
                    CharmData.hasStoredCompanion(stack)
                            ? "item.azscompanions.companion_charm.bound_stored"
                            : "item.azscompanions.companion_charm.bound_active"));
        } else {
            tooltip.accept(Component.translatable("item.azscompanions.companion_charm.unbound"));
        }
    }
}
