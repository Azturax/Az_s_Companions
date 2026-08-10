package com.azscompanions.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Who may trigger which AI world actions.
 * <ul>
 *   <li>{@link #OWNER} — full tool set when {@code enableAiActions}</li>
 *   <li>{@link #STRANGER} — {@link CompanionAiActionNames#isStrangerSafe} only</li>
 *   <li>{@link #NONE} — dialogue only</li>
 * </ul>
 */
public enum CompanionAiActionTrust {
    OWNER,
    STRANGER,
    NONE;

    public boolean allowsActions() {
        return this == OWNER || this == STRANGER;
    }

    public boolean isOwner() {
        return this == OWNER;
    }

    public boolean fullControl() {
        return this == OWNER;
    }

    public static boolean isStrangerSafe(String actionName) {
        return CompanionAiActionNames.isStrangerSafe(actionName);
    }

    public boolean allows(String actionName) {
        return switch (this) {
            case NONE -> false;
            case OWNER -> true;
            case STRANGER -> CompanionAiActionNames.isStrangerSafe(actionName);
        };
    }

    public List<CompanionAiAction> filter(List<CompanionAiAction> actions) {
        if (actions == null || actions.isEmpty() || this == OWNER) {
            return actions == null ? List.of() : actions;
        }
        if (this == NONE) {
            return List.of();
        }
        List<CompanionAiAction> out = new ArrayList<>();
        for (CompanionAiAction a : actions) {
            if (a != null && allows(a.name())) {
                out.add(a);
            }
        }
        return out;
    }

    public static CompanionAiActionTrust forSpeaker(boolean speakerIsOwner) {
        return speakerIsOwner ? OWNER : STRANGER;
    }

    /**
     * Resolve trust for chat/AI. Same FTB team as the owner elevates to {@link #OWNER} when
     * {@code settings.trustSameTeamAsOwner()} is true (and teams compat is on via caller).
     * Otherwise same-team still uses {@link #STRANGER} (helpful social allowlist only).
     */
    public static CompanionAiActionTrust forSpeaker(boolean speakerIsOwner, boolean sameTeamAsOwner,
                                                    CompanionAiSettings settings) {
        if (speakerIsOwner) {
            return OWNER;
        }
        if (sameTeamAsOwner && settings != null && settings.trustSameTeamAsOwner()) {
            return OWNER;
        }
        return STRANGER;
    }
}
