package com.azscompanions.ai;

import java.util.List;
import java.util.UUID;

/**
 * One player → companion chat turn for LLM / MCP providers.
 * {@code speakerIsOwner} selects owner vs stranger prompt / action policy.
 * History is per {@link #companionId()} — never shared across companions.
 * {@link #persona()} is per-companion identity injected into the system prompt.
 * {@link #aiPlayMode()} is the per-companion AI Mode flag (menu / CCI); when true, tools may drive play.
 */
public record CompanionChatContext(
        UUID companionId,
        String companionName,
        String form,
        String attitude,
        String playerName,
        String playerMessage,
        String inputLanguage,
        String parentName,
        boolean child,
        boolean speakerIsOwner,
        List<CompanionChatMemory.Turn> priorTurns,
        CompanionPersona persona,
        boolean aiPlayMode
) {
    public CompanionChatContext(
            String companionName,
            String form,
            String playerName,
            String playerMessage,
            String inputLanguage
    ) {
        this(null, companionName, form, "", playerName, playerMessage, inputLanguage, "", false, true, List.of(),
                CompanionPersona.EMPTY, false);
    }

    public CompanionChatContext(
            String companionName,
            String form,
            String playerName,
            String playerMessage,
            String inputLanguage,
            String parentName,
            boolean child
    ) {
        this(null, companionName, form, "", playerName, playerMessage, inputLanguage, parentName, child, true, List.of(),
                CompanionPersona.EMPTY, false);
    }

    public CompanionChatContext(
            String companionName,
            String form,
            String playerName,
            String playerMessage,
            String inputLanguage,
            String parentName,
            boolean child,
            boolean speakerIsOwner
    ) {
        this(null, companionName, form, "", playerName, playerMessage, inputLanguage, parentName, child, speakerIsOwner,
                List.of(), CompanionPersona.EMPTY, false);
    }

    /** Full context without persona (uses empty defaults). */
    public CompanionChatContext(
            UUID companionId,
            String companionName,
            String form,
            String attitude,
            String playerName,
            String playerMessage,
            String inputLanguage,
            String parentName,
            boolean child,
            boolean speakerIsOwner,
            List<CompanionChatMemory.Turn> priorTurns
    ) {
        this(companionId, companionName, form, attitude, playerName, playerMessage, inputLanguage, parentName, child,
                speakerIsOwner, priorTurns, CompanionPersona.EMPTY, false);
    }

    /** Full context with persona; AI play mode off. */
    public CompanionChatContext(
            UUID companionId,
            String companionName,
            String form,
            String attitude,
            String playerName,
            String playerMessage,
            String inputLanguage,
            String parentName,
            boolean child,
            boolean speakerIsOwner,
            List<CompanionChatMemory.Turn> priorTurns,
            CompanionPersona persona
    ) {
        this(companionId, companionName, form, attitude, playerName, playerMessage, inputLanguage, parentName, child,
                speakerIsOwner, priorTurns, persona, false);
    }

    public CompanionChatContext {
        companionName = companionName == null || companionName.isBlank() ? "Companion" : companionName.trim();
        form = form == null || form.isBlank() ? "player" : form.trim();
        attitude = attitude == null || attitude.isBlank() ? "PASSIVE" : attitude.trim();
        playerName = playerName == null || playerName.isBlank() ? "Player" : playerName.trim();
        playerMessage = playerMessage == null ? "" : playerMessage.trim();
        inputLanguage = inputLanguage == null || inputLanguage.isBlank() ? "en" : inputLanguage.trim();
        parentName = parentName == null ? "" : parentName.trim();
        priorTurns = priorTurns == null ? List.of() : List.copyOf(priorTurns);
        persona = persona == null ? CompanionPersona.EMPTY : persona;
    }

    public CompanionChatContext withPriorTurns(List<CompanionChatMemory.Turn> turns) {
        return new CompanionChatContext(
                companionId, companionName, form, attitude, playerName, playerMessage,
                inputLanguage, parentName, child, speakerIsOwner, turns, persona, aiPlayMode);
    }

    public CompanionChatContext withPersona(CompanionPersona next) {
        return new CompanionChatContext(
                companionId, companionName, form, attitude, playerName, playerMessage,
                inputLanguage, parentName, child, speakerIsOwner, priorTurns, next, aiPlayMode);
    }

    public CompanionChatContext withAiPlayMode(boolean enabled) {
        return new CompanionChatContext(
                companionId, companionName, form, attitude, playerName, playerMessage,
                inputLanguage, parentName, child, speakerIsOwner, priorTurns, persona, enabled);
    }

    /** User message content as sent to the LLM (and stored in per-companion memory). */
    public String formattedUserContent() {
        return playerName + " (" + inputLanguage + "): " + playerMessage;
    }
}
