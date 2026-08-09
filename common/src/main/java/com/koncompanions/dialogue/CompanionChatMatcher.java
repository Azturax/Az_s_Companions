package com.koncompanions.dialogue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Keyword matcher for wholesome companion chat replies.
 * Returns lang keys under {@code dialogue.koncompanions.chat.*}.
 */
public final class CompanionChatMatcher {
    public static final String LANG_PREFIX = "dialogue.koncompanions.chat.";

    private static final List<Topic> TOPICS = List.of(
            // Short greetings need a name address so Kon doesn't reply to every "hi" in multiplayer.
            topic("hello", 3, true, "hello", "hi", "hey", "yo"),
            topic("hello", 3, false, "good morning", "good afternoon", "good evening"),
            topic("how_are_you", 3, false, "how are you", "how're you", "how r u", "how are ya", "you okay", "you alright"),
            topic("name", 2, false, "what's your name", "whats your name", "who are you", "your name", "what is your name"),
            topic("follow", 2, false, "follow me", "come with me", "come here", "come along", "stay with me"),
            topic("sleep", 2, false, "go to bed", "go sleep", "go to sleep", "take a nap", "time for bed", "sleep now"),
            topic("home", 2, false, "where's home", "where is home", "where is your home", "go home", "your home"),
            topic("love", 3, false, "i love you", "love you", "love ya", "i adore you"),
            topic("hungry", 2, false, "are you hungry", "you hungry", "hungry?", "want food", "want a snack"),
            topic("likes", 3, false, "what do you like", "what's your favorite", "whats your favorite", "what do you enjoy", "favorite things"),
            topic("thanks", 2, true, "thanks", "thx", "ty"),
            topic("thanks", 2, false, "thank you"),
            topic("cute", 2, false, "you're cute", "youre cute", "so cute", "you're adorable", "youre adorable", "adorable"),
            topic("goodbye", 3, true, "bye", "later"),
            topic("goodbye", 3, false, "goodbye", "good bye", "see you", "see ya", "good night", "goodnight")
    );

    private static final String[] FALLBACK_KEYS = {
            LANG_PREFIX + "fallback.1",
            LANG_PREFIX + "fallback.2",
            LANG_PREFIX + "fallback.3"
    };

    private CompanionChatMatcher() {
    }

    /**
     * @param message       raw player chat
     * @param companionName custom name or default display name
     * @return lang key to translate, or empty if Kon should stay quiet
     */
    public static Optional<String> match(String message, String companionName) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        String lower = message.toLowerCase(Locale.ROOT).trim();
        boolean addressed = isAddressed(lower, companionName);
        String content = stripAddress(lower, companionName);

        Optional<String> topic = matchTopic(content, addressed);
        if (topic.isPresent()) {
            return topic;
        }
        if (addressed) {
            return Optional.of(pick(FALLBACK_KEYS));
        }
        return Optional.empty();
    }

    /** True when the player is clearly talking to Kon / her custom name. */
    public static boolean isAddressed(String lowerMessage, String companionName) {
        if (lowerMessage == null || lowerMessage.isBlank()) {
            return false;
        }
        String lower = lowerMessage.toLowerCase(Locale.ROOT).trim();
        if (mentionsName(lower, "kon")) {
            return true;
        }
        if (companionName != null && !companionName.isBlank()) {
            String name = companionName.toLowerCase(Locale.ROOT).trim();
            if (!name.equals("kon") && mentionsName(lower, name)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<String> matchTopic(String content, boolean addressed) {
        if (content.isBlank()) {
            return Optional.empty();
        }
        for (Topic topic : TOPICS) {
            if (topic.requiresAddress() && !addressed) {
                continue;
            }
            for (String phrase : topic.phrases()) {
                if (content.equals(phrase) || content.contains(phrase)) {
                    return Optional.of(pickVariant(topic.id(), topic.variants()));
                }
            }
        }
        return Optional.empty();
    }

    private static String stripAddress(String lower, String companionName) {
        String content = lower;
        content = stripLeadingName(content, "kon");
        if (companionName != null && !companionName.isBlank()) {
            content = stripLeadingName(content, companionName.toLowerCase(Locale.ROOT).trim());
        }
        return content.trim();
    }

    private static String stripLeadingName(String lower, String name) {
        if (name.isEmpty()) {
            return lower;
        }
        Pattern leading = Pattern.compile("^" + Pattern.quote(name) + "\\s*[,:\\-]?\\s*", Pattern.CASE_INSENSITIVE);
        return leading.matcher(lower).replaceFirst("");
    }

    private static boolean mentionsName(String lower, String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT).trim();
        if (lower.startsWith(n + " ") || lower.startsWith(n + ",") || lower.startsWith(n + ":")
                || lower.equals(n) || lower.startsWith(n + "-")) {
            return true;
        }
        Pattern word = Pattern.compile("\\b" + Pattern.quote(n) + "\\b", Pattern.CASE_INSENSITIVE);
        return word.matcher(lower).find();
    }

    private static String pickVariant(String topicId, int variants) {
        int index = 1 + ThreadLocalRandom.current().nextInt(Math.max(1, variants));
        return LANG_PREFIX + topicId + "." + index;
    }

    private static String pick(String[] keys) {
        return keys[ThreadLocalRandom.current().nextInt(keys.length)];
    }

    private static Topic topic(String id, int variants, boolean requiresAddress, String... phrases) {
        List<String> sorted = new ArrayList<>(List.of(phrases));
        sorted.sort((a, b) -> Integer.compare(b.length(), a.length()));
        return new Topic(id, variants, requiresAddress, List.copyOf(sorted));
    }

    private record Topic(String id, int variants, boolean requiresAddress, List<String> phrases) {
    }
}
