package com.azscompanions.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-process multi-turn chat buffers keyed by companion entity UUID.
 * Shared server LLM endpoint; separate minds — never merge histories across companions.
 */
public final class CompanionChatMemory {
    public record Turn(String role, String content) {
        public Turn {
            role = role == null ? "user" : role.trim().toLowerCase();
            content = content == null ? "" : content.trim();
        }

        public boolean isUser() {
            return "user".equals(role);
        }

        public boolean isAssistant() {
            return "assistant".equals(role);
        }

        public boolean isBlank() {
            return content.isBlank();
        }
    }

    private final Map<UUID, Deque<Turn>> byCompanion = new ConcurrentHashMap<>();

    public List<Turn> snapshot(UUID companionId, int maxMessages) {
        if (companionId == null || maxMessages <= 0) {
            return List.of();
        }
        Deque<Turn> buf = byCompanion.get(companionId);
        if (buf == null || buf.isEmpty()) {
            return List.of();
        }
        synchronized (buf) {
            List<Turn> all = new ArrayList<>(buf);
            if (all.size() <= maxMessages) {
                return List.copyOf(all);
            }
            return List.copyOf(all.subList(all.size() - maxMessages, all.size()));
        }
    }

    public void recordExchange(UUID companionId, String userContent, String assistantContent, int maxMessages) {
        if (companionId == null || maxMessages <= 0) {
            return;
        }
        Turn user = new Turn("user", userContent);
        Turn assistant = new Turn("assistant", assistantContent);
        if (user.isBlank() && assistant.isBlank()) {
            return;
        }
        Deque<Turn> buf = byCompanion.computeIfAbsent(companionId, id -> new ArrayDeque<>());
        synchronized (buf) {
            if (!user.isBlank()) {
                buf.addLast(user);
            }
            if (!assistant.isBlank()) {
                buf.addLast(assistant);
            }
            while (buf.size() > maxMessages) {
                buf.removeFirst();
            }
        }
    }

    public void clear(UUID companionId) {
        if (companionId != null) {
            byCompanion.remove(companionId);
        }
    }

    public void clearAll() {
        byCompanion.clear();
    }

    /** Test / diagnostics: how many messages stored for one companion. */
    public int size(UUID companionId) {
        if (companionId == null) {
            return 0;
        }
        Deque<Turn> buf = byCompanion.get(companionId);
        if (buf == null) {
            return 0;
        }
        synchronized (buf) {
            return buf.size();
        }
    }
}
