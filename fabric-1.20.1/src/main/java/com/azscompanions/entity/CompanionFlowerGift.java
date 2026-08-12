package com.azscompanions.entity;

import com.azscompanions.ai.CompanionRecentActionMemory;
import com.azscompanions.entity.inventory.FabricCompanionInventory;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;

import java.util.EnumSet;

/**
 * Fabric: gift any {@code #minecraft:flowers} item; companion throws a context-weighted
 * return gift toward the player. Empty-hand right-click is fallback when throw fails.
 */
public final class CompanionFlowerGift {
    private CompanionFlowerGift() {
    }

    public static boolean isFlower(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.FLOWERS);
    }

    public static ItemStack createContextOffer(
            FabricCompanionEntity companion,
            ServerPlayer player,
            RandomSource random) {
        String id = CompanionGiftOfferSupport.pickOfferId(
                buildSnapshot(companion, player),
                random.nextInt(Integer.MAX_VALUE));
        return BuiltInRegistries.ITEM.getOptional(new ResourceLocation(id))
                .map(ItemStack::new)
                .orElseGet(() -> new ItemStack(Items.POPPY));
    }

    public static boolean sameOffer(ItemStack hand, ItemStack offer) {
        return !hand.isEmpty() && !offer.isEmpty() && ItemStack.isSameItemSameTags(hand, offer);
    }

    public static InteractionResult tryGift(
            FabricCompanionEntity companion,
            ServerPlayer player,
            InteractionHand hand,
            ItemStack held) {
        if (!isFlower(held)) {
            return InteractionResult.PASS;
        }
        long now = companion.level().getGameTime();
        if (!CompanionFlowerGiftSupport.canGift(now, companion.getFlowerGiftCooldownUntil())) {
            player.displayClientMessage(Component.translatable("message.azscompanions.flower_cooldown"), true);
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            ItemStack stack = player.getItemInHand(hand);
            stack.shrink(1);
            player.setItemInHand(hand, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }

        ItemStack previousOffer = companion.getOfferedFlower();
        clearOfferFromHands(companion, previousOffer);

        ItemStack offer = createContextOffer(companion, player, companion.getRandom());
        companion.setFlowerGiftCooldownUntil(CompanionFlowerGiftSupport.nextCooldownUntil(now));

        companion.swing(InteractionHand.MAIN_HAND, true);
        companion.level().playSound(null, companion.getX(), companion.getY(), companion.getZ(),
                SoundEvents.CAT_PURR, SoundSource.NEUTRAL, 0.85f, 1.15f);
        if (companion.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    companion.getX(),
                    companion.getY() + companion.getBbHeight() * 0.9d,
                    companion.getZ(),
                    8,
                    0.4d, 0.3d, 0.4d,
                    0.02d);
        }

        if (throwOfferTowardPlayer(companion, player, offer)) {
            companion.setOfferedFlower(ItemStack.EMPTY);
            companion.level().playSound(null, companion.getX(), companion.getY(), companion.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.45f, 1.1f);
        } else {
            companion.setOfferedFlower(offer.copy());
            placeOfferInFreeHand(companion, offer);
        }

        player.displayClientMessage(Component.translatable("message.azscompanions.flower_gifted"), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Empty-hand take of a pending offer when throw delivery failed (does not steal task items).
     */
    public static InteractionResult tryTakeOffer(
            FabricCompanionEntity companion,
            ServerPlayer player,
            InteractionHand hand) {
        ItemStack offer = companion.getOfferedFlower();
        if (offer.isEmpty()) {
            return InteractionResult.PASS;
        }
        ItemStack toGive = offer.copy();
        companion.setOfferedFlower(ItemStack.EMPTY);
        clearOfferFromHands(companion, toGive);

        ItemStack inHand = player.getItemInHand(hand);
        if (inHand.isEmpty()) {
            player.setItemInHand(hand, toGive);
        } else if (!player.getInventory().add(toGive)) {
            player.drop(toGive, false);
        }
        player.displayClientMessage(Component.translatable("message.azscompanions.flower_taken"), true);
        companion.level().playSound(null, companion.getX(), companion.getY(), companion.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f, 1.2f);
        return InteractionResult.CONSUME;
    }

    static boolean throwOfferTowardPlayer(
            FabricCompanionEntity companion, ServerPlayer player, ItemStack offer) {
        if (offer == null || offer.isEmpty() || !(companion.level() instanceof ServerLevel level)) {
            return false;
        }
        double spawnY = companion.getY() + companion.getBbHeight() * 0.65d;
        double[] vel = CompanionFlowerGiftSupport.throwVelocity(
                companion.getX(), spawnY, companion.getZ(),
                player.getX(), player.getY() + player.getBbHeight() * 0.4d, player.getZ());
        ItemEntity entity = new ItemEntity(level, companion.getX(), spawnY, companion.getZ(), offer.copy());
        entity.setDeltaMovement(vel[0], vel[1], vel[2]);
        entity.setPickUpDelay(CompanionFlowerGiftSupport.THROW_PICKUP_DELAY_TICKS);
        return level.addFreshEntity(entity);
    }

    static CompanionGiftOfferSupport.Snapshot buildSnapshot(
            FabricCompanionEntity companion, ServerPlayer player) {
        EnumSet<CompanionGiftOfferSupport.Hint> hints = EnumSet.noneOf(CompanionGiftOfferSupport.Hint.class);
        boolean bathing = CompanionContextSkinSupport.isBathing(
                companion.isSleeping(), companion.isInWaterOrBubble());
        CompanionContextSkinSupport.Context activity = CompanionContextSkinSupport.activeContext(
                companion.getForm().isPlayer(), companion.isSleeping(), bathing, companion.isOwnerExploring());
        if (companion.isSleeping() || activity == CompanionContextSkinSupport.Context.SLEEPING) {
            hints.add(CompanionGiftOfferSupport.Hint.SLEEPING);
        }
        if (bathing || activity == CompanionContextSkinSupport.Context.BATHING) {
            hints.add(CompanionGiftOfferSupport.Hint.BATHING);
        }
        if (activity == CompanionContextSkinSupport.Context.ADVENTURING || companion.isOwnerExploring()) {
            hints.add(CompanionGiftOfferSupport.Hint.ADVENTURING);
            hints.add(CompanionGiftOfferSupport.Hint.OWNER_EXPLORING);
        } else if (companion.isOwnerStandingAround()) {
            hints.add(CompanionGiftOfferSupport.Hint.OWNER_IDLE);
        }

        long now = companion.level().getGameTime();
        CompanionGiftOfferSupport.addRecentActionHints(
                hints, CompanionRecentActionMemory.peek(player.getUUID(), now));

        if (companion.level() instanceof ServerLevel level) {
            int block = level.getBrightness(LightLayer.BLOCK, player.blockPosition());
            int sky = level.getBrightness(LightLayer.SKY, player.blockPosition());
            if (Math.max(block, sky) <= CompanionRecentActionMemory.DARK_LIGHT_THRESHOLD) {
                hints.add(CompanionGiftOfferSupport.Hint.DARKNESS);
            }
            if (level.isNight()) {
                hints.add(CompanionGiftOfferSupport.Hint.NIGHT);
            }
            Holder<Biome> biome = level.getBiome(player.blockPosition());
            CompanionFlowerGiftSupport.addBiomeHints(
                    hints,
                    biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_RIVER),
                    biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_JUNGLE) || biome.is(BiomeTags.IS_TAIGA),
                    biome.is(BiomeTags.IS_NETHER),
                    biome.value().getBaseTemperature());
        }

        if (player.getFoodData().getFoodLevel() <= CompanionGiftOfferSupport.LOW_HUNGER_FOOD_LEVEL) {
            hints.add(CompanionGiftOfferSupport.Hint.LOW_HUNGER);
        }
        if (companion.getAttitude().isHostile()) {
            hints.add(CompanionGiftOfferSupport.Hint.HOSTILE);
        }
        return CompanionGiftOfferSupport.Snapshot.of(hints);
    }

    private static void clearOfferFromHands(FabricCompanionEntity companion, ItemStack offer) {
        if (offer == null || offer.isEmpty()) {
            return;
        }
        FabricCompanionInventory inv = companion.getCompanionInventory();
        if (sameOffer(inv.getMainHand(), offer)) {
            inv.setItem(FabricCompanionInventory.MAIN_HAND, ItemStack.EMPTY);
        }
        if (sameOffer(inv.getOffHand(), offer)) {
            inv.setItem(FabricCompanionInventory.OFF_HAND, ItemStack.EMPTY);
        }
    }

    private static void placeOfferInFreeHand(FabricCompanionEntity companion, ItemStack offer) {
        FabricCompanionInventory inv = companion.getCompanionInventory();
        ItemStack main = inv.getMainHand();
        ItemStack off = inv.getOffHand();
        CompanionFlowerGiftSupport.HandPlacement place = CompanionFlowerGiftSupport.placement(
                main.isEmpty(),
                false,
                off.isEmpty(),
                false);
        switch (place) {
            case MAIN_HAND -> inv.setItem(FabricCompanionInventory.MAIN_HAND, offer.copy());
            case OFF_HAND -> inv.setItem(FabricCompanionInventory.OFF_HAND, offer.copy());
            case PENDING_ONLY -> {
                // Hands busy — pending only until right-click take.
            }
        }
    }
}
