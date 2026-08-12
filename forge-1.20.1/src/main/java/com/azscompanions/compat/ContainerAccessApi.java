package com.azscompanions.compat;

import com.azscompanions.config.CommonConfig;
import com.azscompanions.data.ModTags;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.util.ProtectionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Capability-based container access with anti-duplication / rate limits.
 * Avoids hardcoding only vanilla inventories.
 */
public final class ContainerAccessApi {
    private static final Map<UUID, Long> LAST_ACCESS_TICK = new ConcurrentHashMap<>();

    private ContainerAccessApi() {
    }

    public static void bootstrap() {
        LAST_ACCESS_TICK.clear();
    }

    public static boolean canAccess(ServerLevel level, BlockPos pos, CompanionEntity companion) {
        if (!companion.hasPermission("containers")) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(ModTags.Blocks.BLACKLISTED_BLOCKS)) {
            return false;
        }
        if (!state.is(ModTags.Blocks.ALLOWED_CONTAINERS)
                && itemHandlerAt(level, pos) == null) {
            return false;
        }
        if (ProtectionHelper.isProtectedBlock(level, pos, companion.getOwner())) {
            return ClaimProtectionApi.canModify(level, pos, companion);
        }
        long now = level.getGameTime();
        Long last = LAST_ACCESS_TICK.get(companion.getUUID());
        if (last != null && now - last < CommonConfig.CONTAINER_POLL_COOLDOWN_TICKS.get()) {
            return false;
        }
        LAST_ACCESS_TICK.put(companion.getUUID(), now);
        return true;
    }

    public static ItemStack insert(IItemHandler handler, ItemStack stack, boolean simulate) {
        // Single-threaded server insert — no race with client prediction.
        return ItemHandlerHelper.insertItemStacked(handler, stack, simulate);
    }

    public static ItemStack extract(IItemHandler handler, int slot, int amount, boolean simulate) {
        return handler.extractItem(slot, amount, simulate);
    }

    private static IItemHandler itemHandlerAt(ServerLevel level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be == null) {
            return null;
        }
        return be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
    }

}
