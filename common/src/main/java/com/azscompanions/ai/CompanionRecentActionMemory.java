package com.azscompanions.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Per-owner short-lived event buffer for ambient / reactive companion chatter.
 * TTL + per-kind cooldowns keep lines from spamming.
 * <p>
 * {@link CompanionRecentActionKind#ITEM_FIND} is hard-capped to <strong>at most one</strong>
 * claim per ~14 days (wall-clock <em>and</em> game ticks). The find gate survives
 * {@link #clearPlayer} (logout) so reconnects cannot re-spam finds the same night.
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
    /**
     * Durable find-claim gate (per owner). Not cleared by {@link #clearPlayer} — only
     * {@link #clearAll} (tests / full reset).
     */
    private static final Map<UUID, FindGate> FIND_GATES = new ConcurrentHashMap<>();

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
        FIND_GATES.clear();
        testNowMs = null;
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            BY_PLAYER.remove(playerId);
            // Intentionally keep FIND_GATES — logout must not reset the 14-day find cap.
        }
    }

    /** True when another ITEM_FIND may be claimed for this owner. */
    public static boolean canClaimItemFind(UUID playerId, long gameTime) {
        if (playerId == null) {
            return false;
        }
        FindGate gate = FIND_GATES.get(playerId);
        if (gate == null) {
            return true;
        }
        synchronized (gate) {
            return gate.canClaim(gameTime, nowMs());
        }
    }

    /**
     * Atomically claim the single ITEM_FIND slot for this cooldown window.
     * Shared by builtin finds, custom {@code item_find} fan-out, and direct {@link #record}.
     *
     * @return true if this call owns the slot (at most one success per cooldown window)
     */
    public static boolean tryClaimItemFind(UUID playerId, long gameTime) {
        if (playerId == null) {
            return false;
        }
        FindGate gate = FIND_GATES.computeIfAbsent(playerId, id -> new FindGate());
        synchronized (gate) {
            long now = nowMs();
            if (!gate.canClaim(gameTime, now)) {
                return false;
            }
            gate.lastMs = now;
            gate.lastTick = gameTime;
            return true;
        }
    }

    /**
     * Record an event if per-kind cooldown allows. Reactive kinds can wake early chatter.
     *
     * @return true if stored
     */
    public static boolean record(UUID playerId, long gameTime, CompanionRecentActionKind kind,
                                 String detail, String itemId, boolean reactive) {
        return record(playerId, gameTime, kind, detail, itemId, reactive, false);
    }

    /**
     * @param itemFindAlreadyClaimed when true, {@link CompanionRecentActionKind#ITEM_FIND} skips
     *                               {@link #tryClaimItemFind} (caller already claimed)
     */
    public static boolean record(UUID playerId, long gameTime, CompanionRecentActionKind kind,
                                 String detail, String itemId, boolean reactive,
                                 boolean itemFindAlreadyClaimed) {
        if (playerId == null || kind == null) {
            return false;
        }
        if (kind == CompanionRecentActionKind.ITEM_FIND && !itemFindAlreadyClaimed) {
            if (!tryClaimItemFind(playerId, gameTime)) {
                return false;
            }
            itemFindAlreadyClaimed = true;
        }
        PlayerBuffer buf = BY_PLAYER.computeIfAbsent(playerId, id -> new PlayerBuffer());
        synchronized (buf) {
            buf.prune(gameTime);
            long cool = cooldownTicks(kind);
            Long last = buf.lastRecorded.get(kind);
            if (last != null && gameTime - last < cool) {
                return false;
            }
            CompanionRecentAction action = new CompanionRecentAction(
                    kind, gameTime, detail, itemId, reactive);
            buf.events.add(action);
            buf.lastRecorded.put(kind, gameTime);
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
        return recordDarknessEnter(playerId, gameTime, currentlyDark, true);
    }

    /**
     * @param storeEvent when false, only updates the dark-edge latch (for custom fan-out / gating)
     * @return true when this tick is a dark-<em>enter</em> edge
     */
    public static boolean recordDarknessEnter(
            UUID playerId, long gameTime, boolean currentlyDark, boolean storeEvent) {
        if (playerId == null) {
            return false;
        }
        PlayerBuffer buf = BY_PLAYER.computeIfAbsent(playerId, id -> new PlayerBuffer());
        boolean entered;
        synchronized (buf) {
            boolean was = buf.wasDark;
            buf.wasDark = currentlyDark;
            entered = currentlyDark && !was;
        }
        if (!entered) {
            return false;
        }
        if (!storeEvent) {
            return true;
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
        return peekReactive(playerId, gameTime, a -> true).isPresent();
    }

    public static boolean hasReactive(UUID playerId, long gameTime, Predicate<CompanionRecentAction> allow) {
        return peekReactive(playerId, gameTime, allow).isPresent();
    }

    public static Optional<CompanionRecentAction> peekReactive(UUID playerId, long gameTime) {
        return peekReactive(playerId, gameTime, a -> true);
    }

    public static Optional<CompanionRecentAction> peekReactive(
            UUID playerId, long gameTime, Predicate<CompanionRecentAction> allow) {
        List<CompanionRecentAction> list = peek(playerId, gameTime);
        Predicate<CompanionRecentAction> gate = allow == null ? a -> true : allow;
        return list.stream()
                .filter(CompanionRecentAction::reactive)
                .filter(gate)
                .max(Comparator.comparingInt(CompanionRecentActionMemory::reactivePriority)
                        .thenComparingLong(CompanionRecentAction::gameTime));
    }

    /**
     * Take the highest-priority reactive event and mark it non-reactive (kept for prompt context).
     */
    public static Optional<CompanionRecentAction> consumeReactive(UUID playerId, long gameTime) {
        return consumeReactive(playerId, gameTime, a -> true);
    }

    public static Optional<CompanionRecentAction> consumeReactive(
            UUID playerId, long gameTime, Predicate<CompanionRecentAction> allow) {
        if (playerId == null) {
            return Optional.empty();
        }
        PlayerBuffer buf = BY_PLAYER.get(playerId);
        if (buf == null) {
            return Optional.empty();
        }
        Predicate<CompanionRecentAction> gate = allow == null ? a -> true : allow;
        synchronized (buf) {
            buf.prune(gameTime);
            int bestIdx = -1;
            int bestPri = Integer.MIN_VALUE;
            long bestTime = Long.MIN_VALUE;
            for (int i = 0; i < buf.events.size(); i++) {
                CompanionRecentAction a = buf.events.get(i);
                if (!a.reactive() || !gate.test(a)) {
                    continue;
                }
                int pri = reactivePriority(a);
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

    /** Whether a custom event id is off cooldown for this owner. */
    public static boolean canRecordCustom(UUID playerId, long gameTime, CompanionCustomChatEvent event) {
        if (playerId == null || event == null || !event.isValid()) {
            return false;
        }
        PlayerBuffer buf = BY_PLAYER.get(playerId);
        if (buf == null) {
            return true;
        }
        synchronized (buf) {
            Long last = buf.lastCustom.get(event.id());
            long cool = Math.max(5L, event.cooldownSeconds()) * 20L;
            return last == null || gameTime - last >= cool;
        }
    }

    /**
     * Record a host-defined custom reactive event (cooldown keyed by event id).
     */
    public static boolean recordCustom(UUID playerId, long gameTime, CompanionCustomChatEvent event, String itemId) {
        if (playerId == null || event == null || !event.isValid() || !event.enabled()) {
            return false;
        }
        PlayerBuffer buf = BY_PLAYER.computeIfAbsent(playerId, id -> new PlayerBuffer());
        synchronized (buf) {
            buf.prune(gameTime);
            Long last = buf.lastCustom.get(event.id());
            long cool = Math.max(5L, event.cooldownSeconds()) * 20L;
            if (last != null && gameTime - last < cool) {
                return false;
            }
            String detail = event.prompt().isBlank()
                    ? ("custom event " + event.id())
                    : event.prompt();
            CompanionRecentAction action = new CompanionRecentAction(
                    CompanionRecentActionKind.CUSTOM, gameTime, detail, itemId, true, event.id());
            buf.events.add(action);
            buf.lastCustom.put(event.id(), gameTime);
            while (buf.events.size() > MAX_EVENTS_PER_PLAYER) {
                buf.events.remove(0);
            }
            return true;
        }
    }

    public static long cooldownTicks(CompanionRecentActionKind kind) {
        return switch (kind) {
            case EXPLOSION -> 20L * 180L;
            case DARKNESS -> 20L * 300L;
            case ITEM_CRAFT -> 20L * 180L;
            case CRAFT_READY -> 20L * 240L;
            case ITEM_FIND -> ITEM_FIND_COOLDOWN_TICKS;
            case CUSTOM -> 20L * 180L;
            case DAMAGE, COMBAT -> 20L * 180L;
            case EATING -> 20L * 300L;
            case SLEEPING -> 20L * 300L;
            case BLOCK_PLACE, BLOCK_BREAK -> 20L * 240L;
        };
    }

    private static int reactivePriority(CompanionRecentAction a) {
        if (a.kind() == CompanionRecentActionKind.CUSTOM && a.customEventId() != null) {
            CompanionCustomChatEvent ev = CompanionChatEventSupport.findById(
                    CompanionChatEventSupport.settings(), a.customEventId());
            if (ev != null) {
                return ev.priority();
            }
        }
        return a.kind().priority();
    }

    private static boolean isDefaultReactive(CompanionRecentActionKind kind) {
        return switch (kind) {
            case EXPLOSION, DARKNESS, ITEM_CRAFT, CRAFT_READY, ITEM_FIND, CUSTOM, DAMAGE -> true;
            default -> false;
        };
    }

    private static final class FindGate {
        private long lastMs;
        private long lastTick;

        boolean canClaim(long gameTime, long nowMs) {
            if (lastMs > 0L && nowMs - lastMs < ITEM_FIND_COOLDOWN_MS) {
                return false;
            }
            if (lastTick > 0L && gameTime - lastTick < ITEM_FIND_COOLDOWN_TICKS) {
                return false;
            }
            return true;
        }
    }

    private static final class PlayerBuffer {
        private final List<CompanionRecentAction> events = new ArrayList<>();
        private final Map<CompanionRecentActionKind, Long> lastRecorded = new ConcurrentHashMap<>();
        private final Map<String, Long> lastCustom = new ConcurrentHashMap<>();
        private final java.util.Set<String> seenItemIds = ConcurrentHashMap.newKeySet();
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
