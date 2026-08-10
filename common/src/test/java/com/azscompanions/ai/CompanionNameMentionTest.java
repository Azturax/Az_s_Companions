package com.azscompanions.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionNameMentionTest {
    @Test
    void detectsVocativeAndTokenMentions() {
        assertTrue(CompanionNameMention.messageMentionsName("Bit, come here", "Bit"));
        assertTrue(CompanionNameMention.messageMentionsName("hey Bit please", "Bit"));
        assertTrue(CompanionNameMention.messageMentionsName("BIT dance!", "Bit"));
        assertTrue(CompanionNameMention.messageMentionsName("Kon: hello", "Kon"));
        assertTrue(CompanionNameMention.messageMentionsName("Kon, how are you?", "Kon"));
        assertTrue(CompanionNameMention.messageMentionsName("Bit come here please", "Bit"));
        assertTrue(CompanionNameMention.messageMentionsName(
                "Kon, how are you? Please come here. Then help mine.", "Kon"));
        assertFalse(CompanionNameMention.messageMentionsName("Bitcoin is cool", "Bit"));
        assertFalse(CompanionNameMention.messageMentionsName("hello world", "Bit"));
        assertFalse(CompanionNameMention.messageMentionsName("", "Bit"));
        assertFalse(CompanionNameMention.messageMentionsName("Bit, come here", ""));
    }

    @Test
    void mentionPromptKeepsFullMultiSentenceMessage() {
        String multi = "Kon, how are you? Please come here. Then mine stone.";
        String owner = CompanionNameMention.mentionPrompt("Alex", multi, true);
        assertTrue(owner.contains(multi));
        assertTrue(owner.contains("[owner address]"));
    }

    @Test
    void mentionPromptSelectsOwnerVsStranger() {
        String owner = CompanionNameMention.mentionPrompt("Alex", "Bit, come here", true);
        String stranger = CompanionNameMention.mentionPrompt("Bob", "Bit, hi", false);
        assertTrue(owner.contains("[owner address]"));
        assertTrue(stranger.contains("[stranger address]"));
        assertTrue(stranger.contains("NOT your owner"));
    }
}

class CompanionAiInputTest {
    @Test
    void preservesMultiSentenceAndClamps() {
        String multi = "Hello. Second sentence! Third?";
        assertEquals(multi, CompanionAiInput.normalize(multi, 2000));
        String longMsg = "A".repeat(100) + ". More text here.";
        String clipped = CompanionAiInput.normalize(longMsg, 64);
        assertEquals(64, clipped.length());
        assertTrue(clipped.startsWith("AAAA"));
        assertFalse(clipped.contains("More text"));
        assertTrue(CompanionAiInput.softProgress(System.currentTimeMillis() - 15_000L, 30, System.currentTimeMillis()) > 0.4f);
        assertTrue(CompanionAiInput.softProgress(System.currentTimeMillis(), 30, System.currentTimeMillis()) < 0.1f);
    }
}

class CompanionAiActionNamesStrangerSafeTest {
    @Test
    void strangerSafeAllowlist() {
        assertTrue(CompanionAiActionNames.isStrangerSafe("come_here"));
        assertTrue(CompanionAiActionNames.isStrangerSafe("run_at_player"));
        assertTrue(CompanionAiActionNames.isStrangerSafe("dance"));
        assertTrue(CompanionAiActionNames.isStrangerSafe("peekaboo"));
        assertTrue(CompanionAiActionNames.isStrangerSafe("say"));
        assertTrue(CompanionAiActionNames.isStrangerSafe("play_stop"));
        assertFalse(CompanionAiActionNames.isStrangerSafe("mine"));
        assertFalse(CompanionAiActionNames.isStrangerSafe("follow"));
        assertFalse(CompanionAiActionNames.isStrangerSafe("pickup"));
        assertFalse(CompanionAiActionNames.isStrangerSafe("hide"));
        assertFalse(CompanionAiActionNames.isStrangerSafe(null));
        assertFalse(CompanionAiActionNames.isStrangerSafe(""));
    }

    @Test
    void trustDelegatesToActionNames() {
        assertTrue(CompanionAiActionTrust.isStrangerSafe("dance"));
        assertFalse(CompanionAiActionTrust.isStrangerSafe("mine"));
        assertTrue(CompanionAiActionTrust.STRANGER.allows("come_here"));
        assertFalse(CompanionAiActionTrust.STRANGER.allows("craft"));
        assertTrue(CompanionAiActionTrust.OWNER.fullControl());
        assertFalse(CompanionAiActionTrust.STRANGER.fullControl());
    }
}

class CompanionChatCensorTest {
    @Test
    void censorsDefaultsAndExtraWords() {
        CompanionAiSettings on = new CompanionAiSettings().setCensorChat(true)
                .setCensorExtraWords(List.of("blorp"));
        assertTrue(CompanionChatCensor.censorOutput("what the fuck", on).contains("****"));
        assertEquals("*****", CompanionChatCensor.censorOutput("blorp", on));

        CompanionAiSettings off = new CompanionAiSettings().setCensorChat(false)
                .setCensorExtraWords(List.of("blorp"));
        assertEquals("fuck blorp", CompanionChatCensor.censorOutput("fuck blorp", off));
    }

    @Test
    void maybeCensorViaSettings() {
        CompanionAiSettings on = new CompanionAiSettings().setCensorChat(true);
        CompanionAiSettings off = new CompanionAiSettings().setCensorChat(false);
        assertEquals("****", CompanionChatCensor.censorOutput("fuck", on));
        assertEquals("fuck", CompanionChatCensor.censorOutput("fuck", off));
        assertEquals("****", CompanionProfanityFilter.maybeCensor(true, "fuck"));
        assertEquals("fuck", CompanionProfanityFilter.maybeCensor(false, "fuck"));
    }

    @Test
    void configRoundTripNameListenAndCensor() {
        CompanionAiSettings s = new CompanionAiSettings()
                .setNameListen(false)
                .setCensorChat(false)
                .setCensorExtraWords(List.of("zzz"));
        CompanionAiSettings loaded = CompanionAiConfigIO.fromJson(CompanionAiConfigIO.toJson(s));
        assertFalse(loaded.nameListen());
        assertFalse(loaded.censorChat());
        assertEquals(List.of("zzz"), loaded.censorExtraWords());
    }
}
