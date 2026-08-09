package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanionsFabric;
import com.azscompanions.entity.FabricCompanionEntity;
import com.azscompanions.entity.FabricCompanionMode;
import me.ichun.mods.cci.api.CCIApi;
import me.ichun.mods.cci.api.IApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Applies stream-driven companion behaviours for the Fabric CCI edition.
 */
public final class FabricCciCompanionActions {
    private static final double SEARCH_RANGE = 96.0d;

    private FabricCciCompanionActions() {
    }

    public static void applyOnServer(@Nullable ServerPlayer player, FabricCciCompanionAction action, String message) {
        if (player == null) {
            AzsCompanionsFabric.LOGGER.debug("CCI action {} ignored — no player context", action);
            return;
        }
        FabricCompanionEntity companion = findOwnedCompanion(player);
        if (companion == null) {
            AzsCompanionsFabric.LOGGER.debug("CCI action {} — no owned companion near {}",
                    action, player.getGameProfile().getName());
            toast(player, "No companion nearby", "Summon your companion before using CCI outcomes.");
            return;
        }

        String safe = message == null ? "" : message.trim();
        switch (action) {
            case SAY -> say(player, companion, safe.isEmpty() ? "Hello!" : safe);
            case GREET -> say(player, companion, safe.isEmpty()
                    ? "Thanks for the support!"
                    : "Thanks for the support, " + safe + "!");
            case WAVE -> say(player, companion, safe.isEmpty()
                    ? "Hello there!"
                    : "Hello, " + safe + "!");
            case FOLLOW -> {
                companion.setMode(FabricCompanionMode.FOLLOW);
                companion.getTaskQueue().clear();
                toast(player, companion.getChatDisplayName(), "Following you.");
            }
            case SIT -> {
                companion.setMode(FabricCompanionMode.SIT);
                toast(player, companion.getChatDisplayName(), "Sitting.");
            }
            case STAY -> {
                companion.setMode(FabricCompanionMode.STAY);
                toast(player, companion.getChatDisplayName(), "Staying put.");
            }
        }
    }

    private static void say(ServerPlayer owner, FabricCompanionEntity companion, String line) {
        owner.displayClientMessage(
                Component.literal("<" + companion.getChatDisplayName() + "> " + line),
                false);
        toast(owner, companion.getChatDisplayName(), line);
    }

    private static void toast(ServerPlayer player, String title, String body) {
        try {
            IApi api = CCIApi.getApiImpl();
            if (api != null) {
                api.triggerInformationalToast(Component.literal(title), Component.literal(body));
            }
        } catch (Throwable t) {
            AzsCompanionsFabric.LOGGER.debug("CCI toast unavailable: {}", t.toString());
        }
        player.displayClientMessage(Component.literal(title + " — " + body), true);
    }

    @Nullable
    private static FabricCompanionEntity findOwnedCompanion(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        AABB box = player.getBoundingBox().inflate(SEARCH_RANGE);
        List<FabricCompanionEntity> found = level.getEntitiesOfClass(
                FabricCompanionEntity.class,
                box,
                c -> c.isAlive() && c.isOwnedBy(player));
        return found.stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(player)))
                .orElse(null);
    }
}
