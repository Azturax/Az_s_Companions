package com.azscompanions.compat.cci;

import org.jetbrains.annotations.Nullable;

/**
 * CCI IMC subjects for Az's Companions (Fabric CCI edition).
 */
public enum FabricCciCompanionAction {
    SAY,
    GREET,
    FOLLOW,
    SIT,
    STAY,
    WAVE,
    SET_ATTITUDE,
    SET_TEAM,
    SUMMON,
    SUMMON_PASSIVE,
    SUMMON_HOSTILE,
    SET_EQUIPMENT,
    SET_MAINHAND,
    SET_OFFHAND,
    SET_ARMOR,
    /** Apply form/skin/name/attitude/team/gear to the owner's active called companion. */
    MODIFY,
    /** Brief playful HOSTILE burst, then restore prior attitude. */
    TURN_EVIL;

    @Nullable
    public static FabricCciCompanionAction fromSubject(String subject) {
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
            case "companion_set_attitude", "set_attitude", "attitude" -> SET_ATTITUDE;
            case "companion_set_team", "set_team", "team" -> SET_TEAM;
            case "companion_summon", "summon" -> SUMMON;
            case "companion_summon_passive", "summon_passive" -> SUMMON_PASSIVE;
            case "companion_summon_hostile", "summon_hostile" -> SUMMON_HOSTILE;
            case "companion_set_hand", "companion_set_equipment", "set_equipment", "set_hand" -> SET_EQUIPMENT;
            case "companion_set_mainhand", "set_mainhand", "mainhand" -> SET_MAINHAND;
            case "companion_set_offhand", "set_offhand", "offhand" -> SET_OFFHAND;
            case "companion_set_armor", "set_armor", "armor" -> SET_ARMOR;
            case "companion_modify", "modify", "companion_customize", "customize",
                 "companion_edit", "edit", "set_appearance", "companion_set_appearance" -> MODIFY;
            case "companion_turn_evil", "turn_evil", "go_evil", "berserk", "companion_berserk" -> TURN_EVIL;
            default -> null;
        };
    }

    public boolean isSummon() {
        return this == SUMMON || this == SUMMON_PASSIVE || this == SUMMON_HOSTILE;
    }
}
