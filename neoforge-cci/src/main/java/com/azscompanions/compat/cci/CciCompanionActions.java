package com.azscompanions.compat.cci;

import com.azscompanions.AzsCompanions;
import com.azscompanions.entity.CompanionEntity;
import com.azscompanions.entity.CompanionMode;
import me.ichun.mods.cci.api.CCIApi;
import me.ichun.mods.cci.api.IApi;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * Applies stream-driven companion behaviours for the CCI edition.
 */
public final class CciCompanionActions {
    private static final double SEARCH_RANGE = 96.0d;

    private CciCompanionActions() {
    }

    public static void applyOnServer(@Nullable ServerPlayer player, CciCompanionAction action, String message) {
        if (player == null) {
            AzsCompanions.LOGGER.debug("CCI action {} ignored — no player context", action);
            return;
        }
        CompanionEntity companion = findOwnedCompanion(player);
        if (companion == null) {
            AzsCompanions.LOGGER.debug("CCI action {} — no owned companion near {}", action, player.getGameProfile().getName());
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
                companion.setMode(CompanionMode.FOLLOW);
                companion.getTaskQueue().clear();
                toast(player, companion.getChatDisplayName(), "Following you.");
            }
            case SIT -> {
                companion.setMode(CompanionMode.SIT);
                toast(player, companion.getChatDisplayName(), "Sitting.");
            }
            case STAY -> {
                companion.setMode(CompanionMode.STAY);
                toast(player, companion.getChatDisplayName(), "Staying put.");
            }
        }
    }

    private static void say(ServerPlayer owner, CompanionEntity companion, String line) {
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
            AzsCompanions.LOGGER.debug("CCI toast unavailable: {}", t.toString());
        }
        // Always mirror to action bar so feedback works even if CCI toast API is a dummy.
        player.displayClientMessage(Component.literal(title + " — " + body), true);
    }

    @Nullable
    private static CompanionEntity findOwnedCompanion(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        AABB box = player.getBoundingBox().inflate(SEARCH_RANGE);
        List<CompanionEntity> found = level.getEntitiesOfClass(
                CompanionEntity.class,
                box,
                c -> c.isAlive() && (c.isOwnedBy(player) || c.isTrusted(player)));
        return found.stream()
                .min(Comparator.comparingDouble(c -> c.distanceToSqr(player)))
                .orElse(null);
    }
}
