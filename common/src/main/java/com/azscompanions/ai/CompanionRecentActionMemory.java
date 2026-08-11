package com.azscompanions.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-owner short-lived event buffer for ambient / reactive companion chatter.
 * TTL + per-kind cooldowns keep lines from spamming.
 * {@link CompanionRecentActionKind#ITEM_FIND} uses a long wall-clock cooldown (~2 weeks).
 */
public final class CompanionRecentActionMemory {
    public static final long DEFAULT_TTL_TICKS = 20L * 60L; // 60s
    public static final int MAX_EVENTS_PER_PLAYER = 8;
    public static final int DARK_LIGHT_THRESHOLD = 7;

    /** ~14 days real-time between "nice find" reactions (per owner). */
    public static final long ITEM_FIND_COOLDOWN_MS = 14L * 24L * 60L * 60L * 1000L;
    /** Same span in world ticks at 20 TPS (14×24×60×60×20). */
    public static final long ITEM_FIND_COOLDOWN_TICKS = 20L * 60L * 60L * 24L * 14L; // 24_192_000

    private static final Map<UUID, PlayerBuffer> BY_PLAYER = new ConcurrentHashMap<>();

    /** Test override for wall-clock; null → {@link System#currentTimeMillis()}. */
    static volatile Long testNowMs;

    private CompanionRecentActionMemory() {
    }

    private static long nowMs() {
        Long override = testNowMs;
        return override != null ? override : System.currentTimeMillis();
    }

    public static void clearAll() {
        BY_PLAYER.clear();
        testNowMs = null;
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            BY_PLAYER.remove(playerId);
        }
    }

    /**
     * Record an event if per-kind cooldown allows. Reactive kinds can wake early chatter.
     *
     * @return true if stored
     */
    public static boolean record(UUID playerId, long gameTime, CompanionRecentActionKind kind,
                                 String detail, String itemId, boolean reactive) {
        if (playerId == null || kind == null) {
            return false;
        }
        PlayerBuffer buf = BY_PLAYER.computeIfAbsent(playerId, id -> new PlayerBuffer());
        synchronized (buf) {
            buf.prune(gameTime);
            if (kind == CompanionRecentActionKind.ITEM_FIND) {
                long now = nowMs();
                if (buf.lastFindReactMs > 0L && now - buf.lastFindReactMs < ITEM_FIND_COOLDOWN_MS) {
                    return false;
                }
            }
            long cool = cooldownTicks(kind);
            Long last = buf.lastRecorded.get(kind);
            if (last != null && gameTime - last < cool) {
                return false;
            }
            CompanionRecentAction action = new CompanionRecentAction(
                    kind, gameTime, detail, itemId, reactive);
            buf.events.add(action);
            buf.lastRecorded.put(kind, gameTime);
            if (kind == CompanionRecentActionKind.ITEM_FIND) {
                buf.lastFindReactMs = nowMs();
            }
            while (buf.events.size() > MAX_EVENTS_PER_PLAYER) {
                buf.events.remove(0);
            }
            if (kind == CompanionRecentActionKind.DARKNESS) {
                buf.wasDark = true;
            }
            return true;
        }
    }

    public static boolean record(UUID playerId, long gameTime, CompanionRecentActionKind kind,
                                 String detail, String itemId) {
        return record(playerId, gameTime, kind, detail, itemId, isDefaultReactive(kind));
    }

    /** Edge-trigger darkness: only when entering dark from lit. */
    public static boolean recordDarknessEnter(UUID playerId, long gameTime, boolean currentlyDark) {
        if (playerId == null) {
            return false;
        }
        PlayerBuffer buf = BY_PLAYER.computeIfAbsent(playerId, id -> new PlayerBuffer());
        synchronized (buf) {
            boolean was = buf.wasDark;
            buf.wasDark = currentlyDark;
            if (!currentlyDark || was) {
                return false;
            }
        }
        return record(playerId, gameTime, CompanionRecentActionKind.DARKNESS,
                "it is too dark — ask for a torch or light", null, true);
    }

    public static boolean markFirstOfKind(UUID playerId, String itemId) {
        if (playerId == null || itemId == null || itemId.isBlank()) {
            return false;
        }
        String id = CompanionNotableItemSupport.normalizeId(itemId);
        PlayerBuffer buf = BY_PLAYER.computeIfAbsent(playerId, u -> new PlayerBuffer());
        synchronized (buf) {
            return buf.seenItemIds.add(id);
        }
    }

    public static List<CompanionRecentAction> peek(UUID playerId, long gameTime) {
        if (playerId == null) {
            return List.of();
        }
        PlayerBuffer buf = BY_PLAYER.get(playerId);
        if (buf == null) {
            return List.of();
        }
        synchronized (buf) {
            buf.prune(gameTime);
            return List.copyOf(buf.events);
        }
    }

    public static boolean hasReactive(UUID playerId, long gameTime) {
        return peekReactive(playerId, gameTime).isPresent();
    }

    public static Optional<CompanionRecentAction> peekReactive(UUID playerId, long gameTime) {
        List<CompanionRecentAction> list = peek(playerId, gameTime);
        return list.stream()
                .filter(CompanionRecentAction::reactive)
                .max(Comparator.comparingInt((CompanionRecentAction a) -> a.kind().priority())
                        .thenComparingLong(CompanionRecentAction::gameTime));
    }

    /**
     * Take the highest-priority reactive event and mark it non-reactive (kept for prompt context).
     */
    public static Optional<CompanionRecentAction> consumeReactive(UUID playerId, long gameTime) {
        if (playerId == null) {
            return Optional.empty();
        }
        PlayerBuffer buf = BY_PLAYER.get(playerId);
        if (buf == null) {
            return Optional.empty();
        }
        synchronized (buf) {
            buf.prune(gameTime);
            int bestIdx = -1;
            int bestPri = Integer.MIN_VALUE;
            long bestTime = Long.MIN_VALUE;
            for (int i = 0; i < buf.events.size(); i++) {
                CompanionRecentAction a = buf.events.get(i);
                if (!a.reactive()) {
                    continue;
                }
                int pri = a.kind().priority();
                if (pri > bestPri || (pri == bestPri && a.gameTime() >= bestTime)) {
                    bestPri = pri;
                    bestTime = a.gameTime();
                    bestIdx = i;
                }
            }
            if (bestIdx < 0) {
                return Optional.empty();
            }
            CompanionRecentAction chosen = buf.events.get(bestIdx);
            buf.events.set(bestIdx, chosen.withReactive(false));
            return Optional.of(chosen);
        }
    }

    public static long cooldownTicks(CompanionRecentActionKind kind) {
        return switch (kind) {
            case EXPLOSION -> 20L * 45L;
            case DARKNESS -> 20L * 90L;
            case ITEM_CRAFT -> 20L * 20L;
            case CRAFT_READY -> 20L * 45L;
            case ITEM_FIND -> ITEM_FIND_COOLDOWN_TICKS;
            case DAMAGE, COMBAT -> 20L * 35L;
            case EATING -> 20L * 60L;
            case SLEEPING -> 20L * 90L;
            case BLOCK_PLACE, BLOCK_BREAK -> 20L * 40L;
        };
    }

    private static boolean isDefaultReactive(CompanionRecentActionKind kind) {
        return switch (kind) {
            case EXPLOSION, DARKNESS, ITEM_CRAFT, CRAFT_READY, ITEM_FIND, DAMAGE -> true;
            default -> false;
        };
    }

    private static final class PlayerBuffer {
        private final List<CompanionRecentAction> events = new ArrayList<>();
        private final Map<CompanionRecentActionKind, Long> lastRecorded = new ConcurrentHashMap<>();
        private final java.util.Set<String> seenItemIds = ConcurrentHashMap.newKeySet();
        /** Wall-clock ms of last ITEM_FIND reaction (0 = never). */
        private long lastFindReactMs;
        private boolean wasDark;

        void prune(long gameTime) {
            Iterator<CompanionRecentAction> it = events.iterator();
            while (it.hasNext()) {
                CompanionRecentAction a = it.next();
                if (gameTime - a.gameTime() > DEFAULT_TTL_TICKS) {
                    it.remove();
                }
            }
        }
    }
}
