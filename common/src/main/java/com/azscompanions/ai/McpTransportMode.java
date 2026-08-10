package com.azscompanions.ai;

/**
 * MCP transport for companion AI. Prefer HTTP for dedicated servers;
 * stdio launches a local subprocess (single-player / operator machines).
 */
public enum McpTransportMode {
    HTTP,
    STDIO;

    public static McpTransportMode fromConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return HTTP;
        }
        String key = raw.trim().toLowerCase();
        if (key.equals("stdio") || key.equals("process") || key.equals("subprocess")) {
            return STDIO;
        }
        return HTTP;
    }
}
