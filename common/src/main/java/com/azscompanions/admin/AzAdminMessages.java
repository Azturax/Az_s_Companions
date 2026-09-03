package com.azscompanions.admin;

/**
 * Shared admin / AI-config chat strings (server → player).
 */
public final class AzAdminMessages {
    public static final String DENIED =
            "You don't have permission to use Az admin. Ask the server owner, or get ops / whitelist.";
    public static final String DISABLED =
            "Az admin is disabled on this server (enableAzAdminCommand=false).";
    public static final String AI_SAVED_RESTART =
            "Companion AI settings saved. Restart the server/game for them to apply.";
    /** Preferred after save when runtime was hot-applied. */
    public static final String AI_SAVED_APPLIED =
            "Companion AI settings saved and applied on the server.";
    public static final String AI_PLAYER_SAVED =
            "Your companion AI chat settings were saved.";
    public static final String AI_SAVE_FAILED =
            "Could not save companion AI settings to disk. Check server logs.";
    public static final String AI_INVALID =
            "Invalid AI settings — fix the highlighted fields and try again.";

    private AzAdminMessages() {
    }
}
