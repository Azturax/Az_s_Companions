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
    /** Set whoAmI / whatAmIDoing / howWillIBe (marks personaInitialized). */
    PERSONA,
    TURN_EVIL,
    ASK,
    AI_STATUS,
    AI_CHAT,
    TEAMFIGHT_ENABLE,
    TEAMFIGHT_DISABLE,
    TEAMFIGHT_TOGGLE,
    TEAMFIGHT_STATUS,
    TEAMFIGHT_SCOREBOARD,
    TEAMFIGHT_SCORE,
    TEAMFIGHT_TOP,
    SPAWN_LEADER,
    /**
     * CCI-defined interaction → spawn children under a named leader
     * ({@code amount=}÷price or explicit {@code count=}).
     */
    SPAWN_CHILD,
    /** Store world children onto parent (callable later). */
    DISMISS_CHILD,
    /** Play behaviors via mode= (rush/hide/seek/dance/peekaboo/stop). */
    PLAY,
    /** Alias subject for rush / run_at_player. */
    RUSH,
    /** Alias subject for hide-and-seek (role=hider|seeker). */
    HIDE_SEEK,
    /** FTB Chunks claim at companion feet (owner quota; config + mod required). */
    CLAIM_CHUNK,
    /** FTB Chunks unclaim at companion feet. */
    UNCLAIM_CHUNK,
    /** Session AI hints: chatListenMode / enableAiActions (runtime; not persisted to disk). */
    AI_CONFIG;

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
            case "companion_persona", "persona", "set_persona", "companion_set_persona" -> PERSONA;
            case "companion_turn_evil", "turn_evil", "go_evil", "berserk", "companion_berserk" -> TURN_EVIL;
            case "companion_ask", "companion_ai", "ai_ask", "ask" -> ASK;
            case "companion_ai_status", "ai_status" -> AI_STATUS;
            case "companion_ai_chat", "ai_chat", "stream_chat" -> AI_CHAT;
            case "teamfight_enable", "teamfight_on", "companion_teamfight_enable", "teamfight_enable_on" -> TEAMFIGHT_ENABLE;
            case "teamfight_disable", "teamfight_off", "companion_teamfight_disable" -> TEAMFIGHT_DISABLE;
            case "teamfight_toggle", "companion_teamfight_toggle" -> TEAMFIGHT_TOGGLE;
            case "teamfight_status", "companion_teamfight_status" -> TEAMFIGHT_STATUS;
            case "teamfight_scoreboard", "scoreboard", "companion_teamfight_scoreboard" -> TEAMFIGHT_SCOREBOARD;
            case "teamfight_score", "teamfight_kill", "companion_teamfight_score" -> TEAMFIGHT_SCORE;
            case "teamfight_top", "teamfight_best", "companion_teamfight_top" -> TEAMFIGHT_TOP;
            case "companion_spawn_leader", "spawn_leader", "teamfight_spawn_leader" -> SPAWN_LEADER;
            case "companion_interaction", "support_spawn", "interaction_spawn",
                 "companion_support", "companion_spawn_child", "spawn_child", "spawn_minion",
                 "companion_spawn_minion", "teamfight_spawn_child" -> SPAWN_CHILD;
            case "companion_dismiss_child", "dismiss_child", "store_child", "companion_store_child" -> DISMISS_CHILD;
            case "companion_play", "play",
                 "companion_dance", "dance", "companion_peekaboo", "peekaboo",
                 "play_stop", "companion_play_stop" -> PLAY;
            case "companion_rush", "rush", "run_at_player" -> RUSH;
            case "companion_hide_seek", "hide_seek", "hideandseek", "hide_and_seek" -> HIDE_SEEK;
            case "claim_chunk", "companion_claim_chunk", "claimchunk" -> CLAIM_CHUNK;
            case "unclaim_chunk", "companion_unclaim_chunk", "unclaimchunk" -> UNCLAIM_CHUNK;
            case "companion_ai_config", "ai_config", "set_ai_config", "companion_set_ai" -> AI_CONFIG;
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

    public boolean isAiControl() {
        return this == AI_STATUS || this == AI_CONFIG;
    }
}
