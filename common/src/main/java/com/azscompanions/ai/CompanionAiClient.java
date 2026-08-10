package com.azscompanions.ai;

import java.util.Optional;

/**
 * Produces a companion chat reply. Implementations must be thread-safe for background calls.
 */
public interface CompanionAiClient {
    Optional<String> chat(CompanionAiSettings settings, CompanionChatContext context) throws Exception;
}
