package com.azscompanions.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * Unified OpenAI-compatible {@code POST /v1/chat/completions} client.
 * Works for local Ollama ({@code http://127.0.0.1:11434/v1}), LM Studio, llama.cpp server,
 * OpenAI, OpenRouter, Groq, Together, LiteLLM, Azure OpenAI-compatible proxies, etc.
 */
public final class OpenAiCompatibleClient implements CompanionAiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("azscompanions/ai");

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public Optional<String> chat(CompanionAiSettings settings, CompanionChatContext context) throws Exception {
        String rawBase = settings.baseUrl() == null ? "" : settings.baseUrl().trim();
        if (rawBase.isBlank()) {
            throw new IllegalStateException("Companion AI baseUrl is empty — set baseUrl in azscompanions-ai config");
        }
        // API key is optional: local LiteLLM / open proxies may not require auth.
        // When a key is set, Authorization: Bearer is added; secured proxies return HTTP 401 without one.
        String base = rawBase.replaceAll("/+$", "");
        if (!base.endsWith("/v1")) {
            // Allow either http://host:11434/v1 or http://host:11434
            if (!base.contains("/v1")) {
                base = base + "/v1";
            }
        }
        String url = base + "/chat/completions";
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid Companion AI baseUrl: " + rawBase);
        }

        String model = settings.model() == null ? "" : settings.model().trim();
        boolean gemmaLike = looksLikeGemma(model);
        int maxTokens = settings.maxTokens();
        if (gemmaLike && maxTokens < 512) {
            // Thinking models often burn a small budget on reasoning and leave content empty.
            maxTokens = 512;
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", 0.7);
        if (gemmaLike) {
            // Ollama / LiteLLM / vLLM Gemma 4: disable thinking so content is populated.
            body.addProperty("think", false);
            body.addProperty("reasoning_effort", "none");
            JsonObject kwargs = new JsonObject();
            kwargs.addProperty("enable_thinking", false);
            body.add("chat_template_kwargs", kwargs);
        }
        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", settings.formatSystemPrompt(
                context.companionName(), context.form(), context.parentName(), context.child(),
                context.speakerIsOwner(), context.attitude(), context.persona()));
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

        HttpRequest.Builder req = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));

        LlmHttpAuth.applyBearer(req, settings.resolveApiKey());

        HttpResponse<String> response;
        try {
            response = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.net.ConnectException e) {
            throw new IllegalStateException("LLM connection refused at " + base + " — is the proxy/server running?", e);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new IllegalStateException("LLM request timed out after " + settings.timeoutSeconds() + "s", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM request interrupted", e);
        }
        if (response == null) {
            throw new IllegalStateException("LLM returned no HTTP response");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            LOGGER.warn("LLM HTTP {} body={}", response.statusCode(), truncate(response.body(), 2000));
            throw new IllegalStateException("LLM HTTP " + response.statusCode()
                    + " — check model id / proxy auth (details in server log)");
        }
        Optional<String> text = Optional.ofNullable(extractAssistantText(response.body())).filter(s -> !s.isBlank());
        if (text.isEmpty()) {
            // HTTP OK but no speakable text — usually wrong/empty upstream content, not a mod crash.
            String diagnose = diagnoseEmptyAssistant(response.body());
            LOGGER.warn("LLM empty assistant content (HTTP {}). {}. Body={}",
                    response.statusCode(), diagnose, truncate(response.body(), 2000));
            throw new IllegalStateException(
                    "LLM returned empty assistant content (HTTP " + response.statusCode()
                            + "). " + diagnose
                            + (gemmaLike
                            ? " For Gemma 4, disable thinking in the proxy or raise maxTokens."
                            : " Check model id vs LiteLLM/proxy config."));
        }
        return text;
    }

    static boolean looksLikeGemma(String model) {
        if (model == null || model.isBlank()) {
            return false;
        }
        return model.toLowerCase(Locale.ROOT).contains("gemma");
    }

    /**
     * Compact diagnosis for empty replies — never dumps the raw body (that leaks into /ask chat).
     */
    static String diagnoseEmptyAssistant(String json) {
        if (json == null || json.isBlank()) {
            return "Response body empty.";
        }
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return "No choices[] in response.";
            }
            JsonObject first = choices.get(0).getAsJsonObject();
            String finish = first.has("finish_reason") && first.get("finish_reason").isJsonPrimitive()
                    ? first.get("finish_reason").getAsString() : "?";
            JsonElement messageEl = first.get("message");
            if (messageEl == null || !messageEl.isJsonObject()) {
                return "Missing message object (finish_reason=" + finish + ").";
            }
            JsonObject message = messageEl.getAsJsonObject();
            String contentShape = contentShape(message.get("content"));
            boolean hasReasoning = !CompanionAiActionParser.firstNonBlankPrimitive(
                    message, "reasoning_content", "reasoning", "thinking").isBlank();
            boolean hasTools = message.has("tool_calls") && message.get("tool_calls").isJsonArray()
                    && !message.getAsJsonArray("tool_calls").isEmpty();
            return "content=" + contentShape
                    + ", reasoning=" + (hasReasoning ? "present" : "absent")
                    + ", tool_calls=" + (hasTools ? "yes" : "no")
                    + ", finish_reason=" + finish;
        } catch (RuntimeException e) {
            return "Malformed JSON response.";
        }
    }

    static String contentShape(JsonElement contentEl) {
        if (contentEl == null || contentEl.isJsonNull()) {
            return "null";
        }
        if (contentEl.isJsonPrimitive()) {
            String s = contentEl.getAsString();
            if (s == null || s.isEmpty()) {
                return "empty-string";
            }
            if (s.isBlank()) {
                return "blank-string";
            }
            return "text(" + s.length() + ")";
        }
        if (contentEl.isJsonArray()) {
            return "array(" + contentEl.getAsJsonArray().size() + ")";
        }
        return "object";
    }

    static String extractAssistantText(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalStateException("LLM returned malformed JSON", e);
        }
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
        // Non-chat completions style
        JsonElement text = first.get("text");
        if (text != null && text.isJsonPrimitive()) {
            return text.getAsString().trim();
        }
        // Some proxies put the delta on streaming-shaped objects even for non-stream replies
        JsonElement delta = first.get("delta");
        if (delta != null && delta.isJsonObject()) {
            CompanionAiActionParser.ParsedReply parsed =
                    CompanionAiActionParser.parseMessageObject(delta.getAsJsonObject());
            if (!parsed.speakText().isBlank()) {
                return parsed.speakText();
            }
        }
        return null;
    }

    static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        int limit = Math.max(32, max);
        return s.length() <= limit ? s : s.substring(0, limit) + "…";
    }
}
