package com.azscompanions.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleClientTest {
    @Test
    void extractsAssistantContent() {
        String json = """
                {"choices":[{"message":{"role":"assistant","content":"Hi there!"}}]}
                """;
        assertEquals("Hi there!", OpenAiCompatibleClient.extractAssistantText(json));
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
        assertEquals(LlmProviderMode.MCP, LlmProviderMode.fromConfig("mcp"));
        assertTrue(LlmProviderMode.LOCAL.usesOpenAiCompatibleHttp());
    }
}
