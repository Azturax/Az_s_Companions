package com.azscompanions.ai;

import com.azscompanions.entity.CompanionPlayerAiPrefs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanionChatListenSupportTest {
    @Test
    void ignoresCommandsCompanionAndCci() {
        assertTrue(CompanionChatListenSupport.shouldIgnoreChat("/az ask hi"));
        assertTrue(CompanionChatListenSupport.shouldIgnoreChat("<Kon> hello"));
        assertTrue(CompanionChatListenSupport.shouldIgnoreChat("[BRAT] <Kon> hello"));
        assertTrue(CompanionChatListenSupport.shouldIgnoreChat("[CCI] spawn"));
        assertTrue(CompanionChatListenSupport.shouldIgnoreChat("cci: wave"));
        assertFalse(CompanionChatListenSupport.shouldIgnoreChat("hey Kon, how's it going?"));
        assertFalse(CompanionChatListenSupport.shouldIgnoreChat("hello everyone"));
    }

    @Test
    void nearbyOnlyInGlobalAndNotEveryLine() {
        assertFalse(CompanionChatListenSupport.allowNearbyReply(ChatListenMode.OFF, 0));
        assertFalse(CompanionChatListenSupport.allowNearbyReply(ChatListenMode.PLAYER, 0));
        assertTrue(CompanionChatListenSupport.allowNearbyReply(ChatListenMode.GLOBAL, 0));
        assertTrue(CompanionChatListenSupport.allowNearbyReply(ChatListenMode.GLOBAL, 34));
        assertFalse(CompanionChatListenSupport.allowNearbyReply(ChatListenMode.GLOBAL, 35));
        assertFalse(CompanionChatListenSupport.allowNearbyReply(ChatListenMode.GLOBAL, 99));
    }

    @Test
    void prefsDefaultOnAndCycle() {
        assertEquals(ChatListenMode.GLOBAL, CompanionPlayerAiPrefs.defaultChatListen());
        assertTrue(CompanionPlayerAiPrefs.defaultGlobalTalk());
        assertTrue(CompanionPlayerAiPrefs.replyToChatEnabled(ChatListenMode.GLOBAL));
        assertFalse(CompanionPlayerAiPrefs.replyToChatEnabled(ChatListenMode.OFF));
        assertEquals(ChatListenMode.PLAYER, CompanionPlayerAiPrefs.cycleChatListen(ChatListenMode.OFF));
        assertEquals(ChatListenMode.GLOBAL, CompanionPlayerAiPrefs.cycleChatListen(ChatListenMode.PLAYER));
        assertEquals(ChatListenMode.OFF, CompanionPlayerAiPrefs.cycleChatListen(ChatListenMode.GLOBAL));
        assertEquals(ChatListenMode.GLOBAL, CompanionPlayerAiPrefs.fromReplyToggle(true));
        assertEquals(ChatListenMode.OFF, CompanionPlayerAiPrefs.fromReplyToggle(false));
    }
}
