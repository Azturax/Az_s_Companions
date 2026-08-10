package com.azscompanions.teamfight;

import com.azscompanions.cci.CciCompanionParams;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Optional helper: Twitch-bot chat lines → same key=value message CCI actions accept.
 * Primary API remains CCI IMC; this only translates chat into that format.
 */
public final class TeamFightChatParser {
    private static final Pattern BITS = Pattern.compile("(\\d+)\\s*bits?", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUBS = Pattern.compile("(\\d+)\\s*subs?", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEAM = Pattern.compile("\\b(team\\s*[:=]?\\s*)?(red|blue|left|right)\\b", Pattern.CASE_INSENSITIVE);

    private TeamFightChatParser() {
    }

    /**
     * Best-effort parse of donation chat into a CCI message string.
     * Example in: {@code Alice cheered 500 bits for zombie with diamond_sword on red}
     * Example out: {@code name=Alice;bits=500;form=zombie;mainhand=minecraft:diamond_sword;team=red}
     */
    public static String toCciMessage(String chatLine) {
        if (chatLine == null || chatLine.isBlank()) {
            return "";
        }
        // Already structured?
        if (chatLine.contains("=")) {
            return chatLine.trim();
        }
        String line = chatLine.trim();
        StringBuilder out = new StringBuilder();

        Matcher bits = BITS.matcher(line);
        if (bits.find()) {
            append(out, "bits", bits.group(1));
        }
        Matcher subs = SUBS.matcher(line);
        if (subs.find()) {
            append(out, "subs", subs.group(1));
        }
        Matcher team = TEAM.matcher(line);
        if (team.find()) {
            String t = team.group(2).toLowerCase(Locale.ROOT);
            if ("left".equals(t)) {
                t = TeamFightDefaults.TEAM_LEFT;
            } else if ("right".equals(t)) {
                t = TeamFightDefaults.TEAM_RIGHT;
            }
            append(out, "team", t);
        }

        String lower = line.toLowerCase(Locale.ROOT);
        for (String form : new String[]{
                "zombie", "skeleton", "husk", "stray", "spider", "enderman",
                "chicken", "wolf", "cat", "cow", "pig", "sheep", "fox", "rabbit", "bee", "player"}) {
            if (lower.matches(".*\\b" + form + "\\b.*")) {
                append(out, "form", form);
                break;
            }
        }

        for (String item : new String[]{
                "diamond_sword", "iron_sword", "stone_sword", "wooden_sword", "stick",
                "netherite_sword", "bow", "shield"}) {
            if (lower.contains(item)) {
                append(out, "mainhand", "minecraft:" + item);
                break;
            }
        }

        // First token as name if looks like a username
        String[] words = line.split("\\s+");
        if (words.length > 0 && words[0].matches("[A-Za-z0-9_]{2,16}")) {
            append(out, "name", words[0]);
        }

        return out.toString();
    }

    public static CciCompanionParams parseChatOrMessage(String raw) {
        String msg = raw == null ? "" : raw.trim();
        if (!msg.contains("=")) {
            msg = toCciMessage(msg);
        }
        return CciCompanionParams.parse(msg);
    }

    private static void append(StringBuilder out, String key, String val) {
        if (out.length() > 0) {
            out.append(';');
        }
        out.append(key).append('=').append(val);
    }
}
