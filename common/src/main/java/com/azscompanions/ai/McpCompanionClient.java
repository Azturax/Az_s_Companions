package com.azscompanions.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal MCP client for companion chat.
 * <p>
 * HTTP: Streamable-style JSON-RPC ({@code initialize} → {@code tools/call}) against an MCP endpoint URL.
 * STDIO: launches {@code mcpCommand} + args and speaks newline-delimited JSON-RPC on stdin/stdout.
 * <p>
 * Expects a tool (default {@code companion_chat}) that accepts arguments
 * {@code message}, {@code companion_name}, {@code form}, {@code player_name}, {@code language}
 * and returns text content. The MCP server may wrap any local or remote model behind that tool.
 */
public final class McpCompanionClient implements CompanionAiClient {
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final AtomicInteger nextId = new AtomicInteger(1);

    @Override
    public Optional<String> chat(CompanionAiSettings settings, CompanionChatContext context) throws Exception {
        String tool = settings.mcpToolName();
        if (!settings.isToolAllowed(tool)) {
            throw new IllegalStateException("MCP tool '" + tool + "' is not on the allowlist");
        }
        return switch (settings.mcpTransport()) {
            case HTTP -> chatHttp(settings, context, tool);
            case STDIO -> chatStdio(settings, context, tool);
        };
    }

    private Optional<String> chatHttp(CompanionAiSettings settings, CompanionChatContext context, String tool)
            throws Exception {
        String endpoint = settings.mcpUrl();
        Duration timeout = Duration.ofSeconds(settings.timeoutSeconds());

        JsonObject initParams = new JsonObject();
        initParams.addProperty("protocolVersion", settings.mcpProtocolVersion());
        initParams.add("capabilities", new JsonObject());
        JsonObject clientInfo = new JsonObject();
        clientInfo.addProperty("name", "azscompanions");
        clientInfo.addProperty("version", "0.3.3");
        initParams.add("clientInfo", clientInfo);

        HttpResponse<String> initResponse = postJsonRpc(endpoint, timeout, null, settings.mcpProtocolVersion(),
                rpcRequest("initialize", initParams));
        String sessionId = initResponse.headers().firstValue("Mcp-Session-Id")
                .or(() -> initResponse.headers().firstValue("mcp-session-id"))
                .orElse(null);

        // Best-effort initialized notification (ignore failures).
        try {
            postJsonRpc(endpoint, timeout, sessionId, settings.mcpProtocolVersion(),
                    rpcNotification("notifications/initialized", new JsonObject()));
        } catch (Exception ignored) {
            // Older or sessionless servers may not accept notifications.
        }

        JsonObject callParams = new JsonObject();
        callParams.addProperty("name", tool);
        callParams.add("arguments", toolArguments(settings, context));

        HttpResponse<String> callResponse = postJsonRpc(endpoint, timeout, sessionId, settings.mcpProtocolVersion(),
                rpcRequest("tools/call", callParams));
        if (callResponse.statusCode() < 200 || callResponse.statusCode() >= 300) {
            throw new IllegalStateException("MCP HTTP " + callResponse.statusCode() + ": " + truncate(callResponse.body()));
        }
        return Optional.ofNullable(extractToolText(callResponse.body())).filter(s -> !s.isBlank());
    }

    private Optional<String> chatStdio(CompanionAiSettings settings, CompanionChatContext context, String tool)
            throws Exception {
        if (settings.mcpCommand() == null || settings.mcpCommand().isBlank()) {
            throw new IllegalStateException("MCP stdio requires mcpCommand (executable path)");
        }
        ProcessBuilder pb = new ProcessBuilder();
        List<String> cmd = new java.util.ArrayList<>();
        cmd.add(settings.mcpCommand());
        cmd.addAll(settings.mcpArgs());
        pb.command(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            JsonObject initParams = new JsonObject();
            initParams.addProperty("protocolVersion", settings.mcpProtocolVersion());
            initParams.add("capabilities", new JsonObject());
            JsonObject clientInfo = new JsonObject();
            clientInfo.addProperty("name", "azscompanions");
            clientInfo.addProperty("version", "0.3.3");
            initParams.add("clientInfo", clientInfo);
            writeLine(out, rpcRequest("initialize", initParams));
            readJsonRpcResult(in, settings.timeoutSeconds());

            writeLine(out, rpcNotification("notifications/initialized", new JsonObject()));

            JsonObject callParams = new JsonObject();
            callParams.addProperty("name", tool);
            callParams.add("arguments", toolArguments(settings, context));
            writeLine(out, rpcRequest("tools/call", callParams));
            String resultJson = readJsonRpcResult(in, settings.timeoutSeconds());
            return Optional.ofNullable(extractToolTextFromResult(resultJson)).filter(s -> !s.isBlank());
        } finally {
            process.destroy();
            process.waitFor(2, TimeUnit.SECONDS);
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private JsonObject toolArguments(CompanionAiSettings settings, CompanionChatContext context) {
        JsonObject args = new JsonObject();
        args.addProperty("message", context.playerMessage());
        args.addProperty("companion_name", context.companionName());
        args.addProperty("form", context.form());
        args.addProperty("player_name", context.playerName());
        args.addProperty("language", context.inputLanguage());
        args.addProperty("system_prompt", settings.formatSystemPrompt(context.companionName(), context.form()));
        return args;
    }

    private HttpResponse<String> postJsonRpc(String endpoint, Duration timeout, String sessionId,
                                             String protocolVersion, JsonObject body) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .header("MCP-Protocol-Version", protocolVersion)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        if (sessionId != null && !sessionId.isBlank()) {
            req.header("Mcp-Session-Id", sessionId);
        }
        if (body.has("method")) {
            req.header("Mcp-Method", body.get("method").getAsString());
        }
        if (body.has("params") && body.get("params").isJsonObject()) {
            JsonObject params = body.getAsJsonObject("params");
            if (params.has("name")) {
                req.header("Mcp-Name", params.get("name").getAsString());
            }
        }
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private JsonObject rpcRequest(String method, JsonObject params) {
        JsonObject req = new JsonObject();
        req.addProperty("jsonrpc", "2.0");
        req.addProperty("id", nextId.getAndIncrement());
        req.addProperty("method", method);
        req.add("params", params);
        return req;
    }

    private JsonObject rpcNotification(String method, JsonObject params) {
        JsonObject req = new JsonObject();
        req.addProperty("jsonrpc", "2.0");
        req.addProperty("method", method);
        req.add("params", params);
        return req;
    }

    private static void writeLine(PrintWriter out, JsonObject msg) {
        out.println(msg.toString());
        out.flush();
    }

    private static String readJsonRpcResult(BufferedReader in, int timeoutSeconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            if (!in.ready()) {
                Thread.sleep(25);
                continue;
            }
            String line = in.readLine();
            if (line == null) {
                throw new IllegalStateException("MCP stdio closed unexpectedly");
            }
            line = line.trim();
            if (line.isEmpty() || line.startsWith("event:") || line.startsWith(":")) {
                continue;
            }
            if (line.startsWith("data:")) {
                line = line.substring(5).trim();
            }
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            if (obj.has("error")) {
                throw new IllegalStateException("MCP error: " + obj.get("error"));
            }
            if (obj.has("result")) {
                return obj.get("result").toString();
            }
        }
        throw new IllegalStateException("MCP stdio timed out waiting for result");
    }

    static String extractToolText(String httpBody) {
        String json = unwrapSse(httpBody);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("error")) {
            throw new IllegalStateException("MCP error: " + root.get("error"));
        }
        if (!root.has("result")) {
            return extractToolTextFromResult(json);
        }
        return extractToolTextFromResult(root.get("result").toString());
    }

    static String extractToolTextFromResult(String resultJson) {
        JsonElement el = JsonParser.parseString(resultJson);
        if (el.isJsonPrimitive()) {
            return el.getAsString().trim();
        }
        if (!el.isJsonObject()) {
            return null;
        }
        JsonObject result = el.getAsJsonObject();
        if (result.has("isError") && result.get("isError").getAsBoolean()) {
            throw new IllegalStateException("MCP tool reported error: " + result);
        }
        JsonArray content = result.getAsJsonArray("content");
        if (content == null) {
            if (result.has("message") && result.get("message").isJsonPrimitive()) {
                return result.get("message").getAsString().trim();
            }
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (JsonElement item : content) {
            if (!item.isJsonObject()) {
                continue;
            }
            JsonObject obj = item.getAsJsonObject();
            if (obj.has("text") && obj.get("text").isJsonPrimitive()) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(obj.get("text").getAsString());
            }
        }
        return sb.toString().trim();
    }

    private static String unwrapSse(String body) {
        if (body == null) {
            return "{}";
        }
        if (!body.contains("data:")) {
            return body.trim();
        }
        StringBuilder sb = new StringBuilder();
        for (String line : body.split("\n")) {
            String t = line.trim();
            if (t.startsWith("data:")) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(t.substring(5).trim());
            }
        }
        return sb.length() == 0 ? body.trim() : sb.toString();
    }

    private static String truncate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() <= 240 ? s : s.substring(0, 240) + "…";
    }
}
