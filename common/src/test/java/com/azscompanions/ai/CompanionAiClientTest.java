package com.azscompanions.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
