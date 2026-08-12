package com.azscompanions.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

/**
 * Always appends one item stack when conditions match.
 * Implements {@link IGlobalLootModifier} directly (NeoForge 26.2 LootModifier base ctor moved).
 */
public final class AddItemLootModifier implements IGlobalLootModifier {
    public static final MapCodec<AddItemLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            IGlobalLootModifier.LOOT_CONDITIONS_CODEC.fieldOf("conditions").forGetter(m -> m.conditions),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.item)
    ).apply(inst, AddItemLootModifier::new));

    private final LootItemCondition[] conditions;
    private final Item item;

    public AddItemLootModifier(LootItemCondition[] conditions, Item item) {
        this.conditions = conditions;
        this.item = item;
    }

    @Override
    public ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (LootItemCondition condition : conditions) {
            if (!condition.test(context)) {
                return generatedLoot;
            }
        }
        if (!CompanionLootSupport.isLootInjectionEnabled()) {
            return generatedLoot;
        }
        generatedLoot.add(new ItemStack(item));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    public int priority() {
        return IGlobalLootModifier.DEFAULT_PRIORITY;
    }
}
