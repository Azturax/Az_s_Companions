package com.azscompanions.ai;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Lightweight TCP reachability check for common local LLM endpoints
 * (LiteLLM / Ollama / LM Studio) and an optional configured {@code baseUrl}.
 * No HTTP chat call — consent must happen before connecting for real work.
 */
public final class LlmEndpointProbe {
    public static final int TIMEOUT_MS = 400;

    public record Hit(String profileId, String label, String endpointHint, String baseUrl) {
    }

    private LlmEndpointProbe() {
    }

    /** Probe well-known local OpenAI-compatible ports (+ optional configured local baseUrl). */
    public static Optional<Hit> probeLocal(CompanionAiSettings settings) {
        Map<String, Hit> candidates = new LinkedHashMap<>();
        add(candidates, "litellm", "LiteLLM", "http://127.0.0.1:4000/v1");
        add(candidates, "local_ollama", "Local (Ollama)", CompanionAiSettings.DEFAULT_BASE_URL);
        add(candidates, "local_lm_studio", "Local (LM Studio)", "http://127.0.0.1:1234/v1");
        if (settings != null) {
            String configured = settings.baseUrl();
            if (configured != null && !configured.isBlank() && isLoopbackHttp(configured)) {
                String hint = endpointHint(configured);
                candidates.putIfAbsent(hint, new Hit("custom", "Configured LLM", hint, configured.trim()));
            }
        }
        for (Hit hit : candidates.values()) {
            if (isReachable(hit.baseUrl())) {
                return Optional.of(hit);
            }
        }
        return Optional.empty();
    }

    public static boolean isReachable(String baseUrl) {
        HostPort hp = parseHostPort(baseUrl);
        if (hp == null) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(hp.host, hp.port), TIMEOUT_MS);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String endpointHint(String baseUrl) {
        HostPort hp = parseHostPort(baseUrl);
        if (hp == null) {
            return baseUrl == null ? "" : trimHint(baseUrl);
        }
        return hp.host + ":" + hp.port;
    }

    public static boolean isLoopbackHttp(String baseUrl) {
        HostPort hp = parseHostPort(baseUrl);
        if (hp == null) {
            return false;
        }
        String h = hp.host.toLowerCase(Locale.ROOT);
        return "127.0.0.1".equals(h) || "localhost".equals(h) || "::1".equals(h);
    }

    private static void add(Map<String, Hit> map, String profileId, String label, String baseUrl) {
        String hint = endpointHint(baseUrl);
        map.putIfAbsent(hint, new Hit(profileId, label, hint, baseUrl));
    }

    private static String trimHint(String raw) {
        String s = raw.trim();
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    private static HostPort parseHostPort(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        try {
            String raw = baseUrl.trim();
            if (!raw.contains("://")) {
                raw = "http://" + raw;
            }
            URI uri = URI.create(raw);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            int port = uri.getPort();
            if (port < 0) {
                String scheme = uri.getScheme() == null ? "http" : uri.getScheme().toLowerCase(Locale.ROOT);
                port = "https".equals(scheme) ? 443 : 80;
            }
            return new HostPort(host, port);
        } catch (Exception e) {
            return null;
        }
    }

    /** Stable ordered list of default probe targets (for tests / docs). */
    public static List<String> defaultProbeBaseUrls() {
        List<String> out = new ArrayList<>();
        out.add("http://127.0.0.1:4000/v1");
        out.add(CompanionAiSettings.DEFAULT_BASE_URL);
        out.add("http://127.0.0.1:1234/v1");
        return out;
    }

    private record HostPort(String host, int port) {
    }
}
