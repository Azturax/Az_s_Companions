package com.azscompanions.ai;

import java.net.http.HttpRequest;

/**
 * Shared Authorization header helpers for OpenAI-compatible and MCP HTTP clients.
 * LiteLLM and most OpenAI-compatible proxies expect {@code Authorization: Bearer <key>}.
 */
public final class LlmHttpAuth {
    private LlmHttpAuth() {
    }

    /**
     * Builds a full {@code Authorization} header value, or {@code null} when no key is set.
     * If the raw key already starts with {@code Bearer } (any case), it is not double-prefixed.
     */
    public static String bearerAuthorizationHeader(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        String key = apiKey.trim();
        if (key.length() >= 7 && key.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String rest = key.substring(7).trim();
            return rest.isEmpty() ? null : "Bearer " + rest;
        }
        return "Bearer " + key;
    }

    /** Adds {@code Authorization: Bearer …} when a key is present. */
    public static void applyBearer(HttpRequest.Builder req, String apiKey) {
        if (req == null) {
            return;
        }
        String header = bearerAuthorizationHeader(apiKey);
        if (header != null) {
            req.header("Authorization", header);
        }
    }
}
