package com.azscompanions.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * Unified OpenAI-compatible {@code POST /v1/chat/completions} client.
 * Works for local Ollama ({@code http://127.0.0.1:11434/v1}), LM Studio, llama.cpp server,
 * OpenAI, OpenRouter, Groq, Together, Azure OpenAI-compatible proxies, etc.
 */
public final class OpenAiCompatibleClient implements CompanionAiClient {
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public Optional<String> chat(CompanionAiSettings settings, CompanionChatContext context) throws Exception {
        String base = settings.baseUrl().replaceAll("/+$", "");
        if (!base.endsWith("/v1")) {
            // Allow either http://host:11434/v1 or http://host:11434
            if (!base.contains("/v1")) {
                base = base + "/v1";
            }
        }
        String url = base + "/chat/completions";

        JsonObject body = new JsonObject();
        body.addProperty("model", settings.model());
        body.addProperty("max_tokens", settings.maxTokens());
        body.addProperty("temperature", 0.7);
        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", settings.formatSystemPrompt(
                context.companionName(), context.form(), context.parentName(), context.child(),
                context.speakerIsOwner(), context.attitude(), context.persona(), context.aiPlayMode()));
        messages.add(system);
        for (CompanionChatMemory.Turn turn : context.priorTurns()) {
            if (turn == null || turn.isBlank()) {
                continue;
            }
            String role = turn.isAssistant() ? "assistant" : "user";
            JsonObject prior = new JsonObject();
            prior.addProperty("role", role);
            prior.addProperty("content", turn.content());
            messages.add(prior);
        }
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", context.formattedUserContent());
        messages.add(user);
        body.add("messages", messages);

        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));

        String key = settings.resolveApiKey();
        if (!key.isBlank()) {
            req.header("Authorization", "Bearer " + key);
        }

        HttpResponse<String> response = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("LLM HTTP " + response.statusCode() + ": " + truncate(response.body()));
        }
        return Optional.ofNullable(extractAssistantText(response.body())).filter(s -> !s.isBlank());
    }

    static String extractAssistantText(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        JsonObject first = choices.get(0).getAsJsonObject();
        JsonElement message = first.get("message");
        if (message != null && message.isJsonObject()) {
            CompanionAiActionParser.ParsedReply parsed =
                    CompanionAiActionParser.parseMessageObject(message.getAsJsonObject());
            if (parsed.hasActions()) {
                StringBuilder sb = new StringBuilder(parsed.speakText());
                sb.append("\n```json\n{\"actions\":[");
                boolean firstAction = true;
                for (CompanionAiAction action : parsed.actions()) {
                    if (!firstAction) {
                        sb.append(',');
                    }
                    firstAction = false;
                    sb.append("{\"name\":\"").append(action.name()).append('\"');
                    for (var e : action.args().entrySet()) {
                        sb.append(",\"").append(e.getKey()).append("\":\"")
                                .append(e.getValue().replace("\"", "\\\"")).append('\"');
                    }
                    sb.append('}');
                }
                sb.append("]}\n```");
                return sb.toString().trim();
            }
            if (!parsed.speakText().isBlank()) {
                return parsed.speakText();
            }
        }
        JsonElement text = first.get("text");
        if (text != null && text.isJsonPrimitive()) {
            return text.getAsString().trim();
        }
        return null;
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 240 ? s : s.substring(0, 240) + "…";
    }
}
