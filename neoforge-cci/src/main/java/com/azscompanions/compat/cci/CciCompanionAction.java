package com.azscompanions.compat.cci;

import javax.annotation.Nullable;

public enum CciCompanionAction {
    SAY,
    GREET,
    FOLLOW,
    SIT,
    STAY,
    WAVE;

    @Nullable
    public static CciCompanionAction fromSubject(String subject) {
        if (subject == null || subject.isBlank()) {
            return null;
        }
        return switch (subject.trim().toLowerCase()) {
            case "companion_say", "say" -> SAY;
            case "companion_greet", "greet" -> GREET;
            case "companion_follow", "follow" -> FOLLOW;
            case "companion_sit", "sit" -> SIT;
            case "companion_stay", "stay" -> STAY;
            case "companion_wave", "wave" -> WAVE;
            default -> null;
        };
    }
}
