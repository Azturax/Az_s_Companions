package com.azscompanions.ai;

/**
 * Canonical AI tool / action names the companion can execute when {@code enableAiActions} is on.
 */
public final class CompanionAiActionNames {
    public static final String GOTO = "goto";
    public static final String FOLLOW = "follow";
    public static final String STOP = "stop";
    public static final String STAY = "stay";
    public static final String SIT = "sit";
    public static final String WANDER = "wander";
    public static final String COME_HERE = "come_here";

    public static final String MINE = "mine";
    public static final String PLACE = "place";
    public static final String BUILD = "build";
    public static final String CRAFT = "craft";

    public static final String PICKUP = "pickup";
    public static final String TAKE = "take";
    public static final String USE_ITEM = "use_item";
    public static final String EQUIP = "equip";
    public static final String MOVE_ITEM = "move_item";
    public static final String DROP = "drop";
    public static final String SELECT_HOTBAR = "select_hotbar";

    public static final String RUN_AT_PLAYER = "run_at_player";
    public static final String HIDE = "hide";
    public static final String SEEK = "seek";
    public static final String HIDE_AND_SEEK = "hide_and_seek";
    public static final String DANCE = "dance";
    public static final String PEEKABOO = "peekaboo";
    public static final String PLAY_STOP = "play_stop";

    public static final String SAY = "say";

    public static final String CLAIM_CHUNK = "claim_chunk";
    public static final String UNCLAIM_CHUNK = "unclaim_chunk";

    private CompanionAiActionNames() {
    }

    /**
     * Safe for non-owner speakers: brief social approach / emote / speak only.
     * Blocks grief (mine/build), inventory, permanent follow/stay, etc.
     */
    public static boolean isStrangerSafe(String actionName) {
        if (actionName == null || actionName.isBlank()) {
            return false;
        }
        return switch (actionName.toLowerCase()) {
            case COME_HERE, RUN_AT_PLAYER, DANCE, PEEKABOO, SAY, PLAY_STOP -> true;
            default -> false;
        };
    }

    public static String toolsPromptAppendix() {
        return """
                You may control your body with a JSON action block at the end of your reply:
                ```json
                {"actions":[{"name":"follow"},{"name":"mine","x":10,"y":64,"z":-3},{"name":"craft","item":"minecraft:stick"},{"name":"place","x":11,"y":64,"z":-3,"item":"minecraft:oak_planks"},{"name":"run_at_player"},{"name":"hide"},{"name":"seek"},{"name":"dance"},{"name":"pickup"},{"name":"use_item"},{"name":"equip","slot":"mainhand","from":0},{"name":"drop","slot":27},{"name":"come_here"},{"name":"goto","x":0,"y":64,"z":0},{"name":"claim_chunk"},{"name":"unclaim_chunk"},{"name":"stop"}]}
                ```
                Rules: ownership only; stay within reach (~5 blocks for mine/place); prefer short replies then actions. Unknown tools are ignored.
                claim_chunk / unclaim_chunk (when enabled): claim or release the chunk at your feet or at x/z for the owner using their FTB Chunks quota — never steal others' claims. Walking into claims is fine; do not mine/build in foreign claims.
                """;
    }

    /** Limited tools for stranger (non-owner) name mentions when actions are enabled. */
    public static String strangerToolsPromptAppendix() {
        return """
                Limited social actions only (speaker is not your owner). You may append:
                ```json
                {"actions":[{"name":"come_here"},{"name":"run_at_player"},{"name":"dance"},{"name":"peekaboo"},{"name":"say","text":"Hi!"}]}
                ```
                Allowed: brief approach/face the speaker, dance/peekaboo, say. Forbidden: mine, place, build, craft, follow, stay, sit, wander, goto, pickup, equip, drop, inventory, hide/seek far away, claim_chunk, unclaim_chunk.
                """;
    }
}
