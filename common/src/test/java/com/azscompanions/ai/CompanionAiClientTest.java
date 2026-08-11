package com.azscompanions.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleClientTest {
    @Test
    void extractsAssistantContent() {
        String json = """
                {"choices":[{"message":{"role":"assistant","content":"Hi there!"}}]}
                """;
        assertEquals("Hi there!", OpenAiCompatibleClient.extractAssistantText(json));
    }

    @Test
    void extractsArrayContentParts() {
        String json = """
                {"choices":[{"message":{"role":"assistant","content":[{"type":"text","text":"Hello"},{"type":"text","text":"world"}]}}]}
                """;
        assertEquals("Hello\nworld", OpenAiCompatibleClient.extractAssistantText(json));
    }

    @Test
    void extractsRefusalWhenContentNull() {
        String json = """
                {"choices":[{"message":{"role":"assistant","content":null,"refusal":"No can do"}}]}
                """;
        assertEquals("No can do", OpenAiCompatibleClient.extractAssistantText(json));
    }

    @Test
    void extractsReasoningContentWhenContentEmpty() {
        String json = """
                {"choices":[{"message":{"role":"assistant","content":"","reasoning_content":"Final answer: hi!"},"finish_reason":"length"}]}
                """;
        assertEquals("Final answer: hi!", OpenAiCompatibleClient.extractAssistantText(json));
    }

    @Test
    void emptyContentYieldsNull() {
        String json = """
                {"choices":[{"message":{"role":"assistant","content":""}}]}
                """;
        assertNull(OpenAiCompatibleClient.extractAssistantText(json));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(IllegalStateException.class, () -> OpenAiCompatibleClient.extractAssistantText("{not-json"));
    }

    @Test
    void diagnoseReportsNullContentShape() {
        String json = """
                {"choices":[{"finish_reason":"length","message":{"role":"assistant","content":null}}]}
                """;
        String d = OpenAiCompatibleClient.diagnoseEmptyAssistant(json);
        assertTrue(d.contains("content=null"), d);
        assertTrue(d.contains("finish_reason=length"), d);
        assertFalse(d.contains("{"), d);
    }

    @Test
    void looksLikeGemmaDetectsModelIds() {
        assertTrue(OpenAiCompatibleClient.looksLikeGemma("google/gemma-4-e4b"));
        assertFalse(OpenAiCompatibleClient.looksLikeGemma("gpt-4o-mini"));
    }

    @Test
    void effectiveMaxTokensCapsBackground() {
        assertEquals(128, CompanionAiInput.effectiveMaxTokens(256, true, false));
        assertEquals(256, CompanionAiInput.effectiveMaxTokens(256, false, false));
        assertEquals(512, CompanionAiInput.effectiveMaxTokens(256, false, true));
        assertEquals(128, CompanionAiInput.effectiveMaxTokens(256, true, true));
    }
}

class CompanionAiChatSupportErrorTest {
    @Test
    void playerFacingErrorStripsBodyDump() {
        Throwable err = new IllegalStateException(
                "LLM returned empty assistant content (HTTP 200). Check model. Body: "
                        + "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":null}}]}");
        String msg = CompanionAiChatSupport.playerFacingAiError(err);
        assertTrue(msg.startsWith("Companion AI error:"), msg);
        assertFalse(msg.contains("\"role\""), msg);
        assertFalse(msg.contains("Body:"), msg);
        assertTrue(msg.length() < 200, msg);
    }

    @Test
    void playerFacingErrorKeepsShortMessages() {
        String msg = CompanionAiChatSupport.playerFacingAiError(
                new IllegalStateException("LLM connection refused at http://127.0.0.1:4000/v1"));
        assertEquals(
                "Companion AI error: LLM connection refused at http://127.0.0.1:4000/v1",
                msg);
    }
}

class LlmHttpAuthTest {
    @Test
    void prefixesBearerOnce() {
        assertEquals("Bearer sk-test", LlmHttpAuth.bearerAuthorizationHeader("sk-test"));
        assertEquals("Bearer sk-test", LlmHttpAuth.bearerAuthorizationHeader("Bearer sk-test"));
        assertEquals("Bearer sk-test", LlmHttpAuth.bearerAuthorizationHeader("bearer sk-test"));
        assertNull(LlmHttpAuth.bearerAuthorizationHeader(""));
        assertNull(LlmHttpAuth.bearerAuthorizationHeader(null));
    }
}

class McpCompanionClientTest {
    @Test
    void extractsToolTextContent() {
        String json = """
                {"result":{"content":[{"type":"text","text":"Hello from MCP"}],"isError":false}}
                """;
        assertEquals("Hello from MCP", McpCompanionClient.extractToolText(json));
    }

    @Test
    void providerAliases() {
        assertEquals(LlmProviderMode.LOCAL, LlmProviderMode.fromConfig("ollama"));
        assertEquals(LlmProviderMode.OPENAI_COMPATIBLE, LlmProviderMode.fromConfig("openrouter"));
        assertEquals(LlmProviderMode.OPENAI_COMPATIBLE, LlmProviderMode.fromConfig("litellm"));
        assertEquals(LlmProviderMode.MCP, LlmProviderMode.fromConfig("mcp"));
        assertTrue(LlmProviderMode.LOCAL.usesOpenAiCompatibleHttp());
    }
}
