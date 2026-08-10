package com.azscompanions.ai;

/**
 * One player → companion chat turn for LLM / MCP providers.
 */
public record CompanionChatContext(
        String companionName,
        String form,
        String playerName,
        String playerMessage,
        String inputLanguage
) {
    public CompanionChatContext {
        companionName = companionName == null || companionName.isBlank() ? "Companion" : companionName.trim();
        form = form == null || form.isBlank() ? "player" : form.trim();
        playerName = playerName == null || playerName.isBlank() ? "Player" : playerName.trim();
        playerMessage = playerMessage == null ? "" : playerMessage.trim();
        inputLanguage = inputLanguage == null || inputLanguage.isBlank() ? "en" : inputLanguage.trim();
    }
}
