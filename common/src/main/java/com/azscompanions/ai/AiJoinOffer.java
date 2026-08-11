package com.azscompanions.ai;

/**
 * Join-time offer describing a reachable / configured LLM for the consent prompt.
 * Built on the server from live config, or on the client from a local probe.
 */
public final class AiJoinOffer {
    public static final String SOURCE_SERVER = "server";
    public static final String SOURCE_LOCAL = "local";

    private final boolean available;
    private final String source;
    private final String providerLabel;
    private final String endpointHint;
    private final String suggestProfile;
    private final boolean allowApply;
    private final boolean allowLocalProbe;

    public AiJoinOffer(
            boolean available,
            String source,
            String providerLabel,
            String endpointHint,
            String suggestProfile,
            boolean allowApply,
            boolean allowLocalProbe
    ) {
        this.available = available;
        this.source = source == null || source.isBlank() ? SOURCE_SERVER : source;
        this.providerLabel = providerLabel == null ? "" : providerLabel;
        this.endpointHint = endpointHint == null ? "" : endpointHint;
        this.suggestProfile = suggestProfile == null ? "" : suggestProfile;
        this.allowApply = allowApply;
        this.allowLocalProbe = allowLocalProbe;
    }

    /** S2C snapshot from the live server AI runtime. */
    public static AiJoinOffer fromServerRuntime(CompanionAiRuntime runtime, boolean dedicatedServer) {
        CompanionAiSettings s = runtime == null ? new CompanionAiSettings() : runtime.settings();
        boolean enabled = runtime != null && runtime.isEnabled();
        boolean allowProbe = !dedicatedServer;
        if (!enabled) {
            // SP/integrated: optional local probe — does not favor server LLM
            return new AiJoinOffer(false, SOURCE_SERVER, "", "", "", !dedicatedServer, allowProbe);
        }
        // Only prompt about the *server* LLM when sharing is on, or on dedicated (host process
        // is the only ask path). Personal SP/integrated configs (serverLlmOnly off) stay quiet.
        boolean offerServerLlm = dedicatedServer || s.serverLlmOnly();
        if (!offerServerLlm) {
            return new AiJoinOffer(false, SOURCE_SERVER, "", "", "", false, false);
        }
        String label = s.provider().name().toLowerCase();
        String hint = LlmEndpointProbe.endpointHint(s.baseUrl());
        String profile = "";
        try {
            profile = com.azscompanions.admin.LlmProviderProfile
                    .detect(com.azscompanions.admin.AdminAiConfigSnapshot.fromSettings(s))
                    .name()
                    .toLowerCase();
        } catch (Exception ignored) {
            // keep empty
        }
        // Integrated host Yes → opt into serverLlmOnly; dedicated has no client LLM path.
        return new AiJoinOffer(true, SOURCE_SERVER, label, hint, profile, !dedicatedServer, false);
    }

    public static AiJoinOffer fromLocalProbe(LlmEndpointProbe.Hit hit) {
        if (hit == null) {
            return none();
        }
        return new AiJoinOffer(
                true,
                SOURCE_LOCAL,
                hit.label(),
                hit.endpointHint(),
                hit.profileId(),
                true,
                false);
    }

    public static AiJoinOffer none() {
        return new AiJoinOffer(false, SOURCE_SERVER, "", "", "", false, false);
    }

    public boolean available() {
        return available;
    }

    public String source() {
        return source;
    }

    public String providerLabel() {
        return providerLabel;
    }

    public String endpointHint() {
        return endpointHint;
    }

    public String suggestProfile() {
        return suggestProfile;
    }

    public boolean allowApply() {
        return allowApply;
    }

    public boolean allowLocalProbe() {
        return allowLocalProbe;
    }

    public String promptTitle() {
        return "Companion AI";
    }

    public String promptMessage() {
        String where = endpointHint.isBlank() ? providerLabel : (providerLabel + " @ " + endpointHint);
        if (where.isBlank()) {
            where = "a local LLM";
        }
        if (SOURCE_LOCAL.equals(source)) {
            return "Found a running LLM (" + where + ").\n"
                    + "Optional: connect it for companion chat (/ask)?\n"
                    + "No keeps AI off — you can still set any local or remote LLM in /az admin → AI Config.";
        }
        return "This world/server offers a shared Companion AI (" + where + ").\n"
                + "Optional: use the server LLM for companion chat?\n"
                + "No leaves Use server LLM off — on singleplayer/LAN you can use your own LLM in AI Config instead.";
    }
}
