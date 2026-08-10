package com.azscompanions.compat.cci;

import org.jetbrains.annotations.Nullable;

/**
 * CCI IMC subjects for Az's Companions (Fabric CCI edition).
 * Team-fight actions are first-class CCI subjects (primary streamer API).
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
    MODIFY,
    TURN_EVIL,
    TEAMFIGHT_ENABLE,
    TEAMFIGHT_DISABLE,
    TEAMFIGHT_TOGGLE,
    TEAMFIGHT_STATUS,
    TEAMFIGHT_SCOREBOARD,
    TEAMFIGHT_SCORE,
    TEAMFIGHT_TOP,
    SPAWN_LEADER,
    SPAWN_CHILD;

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
            case "teamfight_enable", "teamfight_on", "companion_teamfight_enable", "teamfight_enable_on" -> TEAMFIGHT_ENABLE;
            case "teamfight_disable", "teamfight_off", "companion_teamfight_disable" -> TEAMFIGHT_DISABLE;
            case "teamfight_toggle", "companion_teamfight_toggle" -> TEAMFIGHT_TOGGLE;
            case "teamfight_status", "companion_teamfight_status" -> TEAMFIGHT_STATUS;
            case "teamfight_scoreboard", "scoreboard", "companion_teamfight_scoreboard" -> TEAMFIGHT_SCOREBOARD;
            case "teamfight_score", "teamfight_kill", "companion_teamfight_score" -> TEAMFIGHT_SCORE;
            case "teamfight_top", "teamfight_best", "companion_teamfight_top" -> TEAMFIGHT_TOP;
            case "companion_spawn_leader", "spawn_leader", "teamfight_spawn_leader", "sub_spawn_leader" -> SPAWN_LEADER;
            case "companion_spawn_child", "spawn_child", "spawn_minion", "companion_spawn_minion",
                 "spawn_bit", "companion_spawn_bit", "bit_spawn", "teamfight_spawn_child" -> SPAWN_CHILD;
            default -> null;
        };
    }

    public boolean isSummon() {
        return this == SUMMON || this == SUMMON_PASSIVE || this == SUMMON_HOSTILE;
    }

    public boolean isTeamFightControl() {
        return this == TEAMFIGHT_ENABLE || this == TEAMFIGHT_DISABLE || this == TEAMFIGHT_TOGGLE
                || this == TEAMFIGHT_STATUS || this == TEAMFIGHT_SCOREBOARD || this == TEAMFIGHT_SCORE
                || this == TEAMFIGHT_TOP || this == SPAWN_LEADER;
    }
}
