package com.azscompanions.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight word-list censor for companion AI chat (outputs always when enabled;
 * stranger inputs can be filtered more aggressively).
 */
public final class CompanionChatCensor {
    /** Common crude terms — keep short; servers can extend via {@code censorExtraWords}. */
    private static final String[] DEFAULT_WORDS = {
            "fuck", "fucking", "fucker", "shit", "shitty", "asshole", "bitch", "bastard",
            "dick", "cock", "pussy", "cunt", "whore", "slut", "nigger", "nigga", "faggot",
            "retard", "retarded", "motherfucker", "bullshit", "damn", "dammit", "crap"
    };

    private CompanionChatCensor() {
    }

    public static String censorOutput(String text, CompanionAiSettings settings) {
        if (settings == null || !settings.censorChat()) {
            return text;
        }
        return censor(text, settings.censorExtraWords());
    }

    /** Stronger pass for non-owner prompts before they reach the LLM. */
    public static String censorStrangerInput(String text, CompanionAiSettings settings) {
        if (settings == null || !settings.censorChat()) {
            return text;
        }
        return censor(text, settings.censorExtraWords());
    }

    public static String censor(String text, List<String> extraWords) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        for (String word : mergedWords(extraWords)) {
            if (word.length() < 2) {
                continue;
            }
            result = replaceWholeWord(result, word);
        }
        return result;
    }

    private static List<String> mergedWords(List<String> extra) {
        Set<String> set = new LinkedHashSet<>();
        for (String w : DEFAULT_WORDS) {
            set.add(w.toLowerCase(Locale.ROOT));
        }
        if (extra != null) {
            for (String w : extra) {
                if (w != null && !w.isBlank()) {
                    set.add(w.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return new ArrayList<>(set);
    }

    private static String replaceWholeWord(String text, String word) {
        Pattern p = Pattern.compile("(?i)(?<!\\p{L})" + Pattern.quote(word) + "(?!\\p{L})");
        Matcher m = p.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement("*".repeat(m.group().length())));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
