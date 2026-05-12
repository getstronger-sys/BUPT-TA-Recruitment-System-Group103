package bupt.ta.llm;

import bupt.ta.model.AiApiSettings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * DeepSeek Chat API (OpenAI-compatible: POST /v1/chat/completions).
 * <p>
 * Two configuration paths are supported:
 * <ol>
 *   <li>Admin-managed JSON (preferred for the web app): build via
 *       {@link #fromAdminSettings(AiApiSettings)}. This bypasses environment variables entirely
 *       so the admin UI is the single source of truth at runtime.</li>
 *   <li>Environment variables / JVM properties (kept for local CLI use and integration tests):
 *       use the no-arg {@link #DeepSeekClient()} constructor. Recognised keys:
 *       {@code TA_AI_API_KEY} / {@code DEEPSEEK_API_KEY},
 *       {@code TA_AI_BASE_URL} / {@code DEEPSEEK_API_BASE},
 *       {@code TA_AI_MODEL} / {@code DEEPSEEK_MODEL},
 *       {@code TA_AI_ENABLED}, {@code TA_AI_PROVIDER}.</li>
 * </ol>
 * Never commit API keys; use env vars, CI secrets, or the admin settings file.
 */
public final class DeepSeekClient {

    public static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    public static final String DEFAULT_MODEL = "deepseek-chat";

    private final boolean enabled;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;

    /**
     * Environment-driven construction. Reads {@code TA_AI_*} / {@code DEEPSEEK_*} env vars and
     * JVM system properties. Used by integration tests and stand-alone CLIs.
     */
    public DeepSeekClient() {
        this(
                isEnabledFromEnv(),
                resolveApiKey(),
                firstNonBlank(System.getenv("TA_AI_BASE_URL"), System.getenv("DEEPSEEK_API_BASE"),
                        System.getProperty("deepseek.api.base"), DEFAULT_BASE_URL),
                firstNonBlank(System.getenv("TA_AI_MODEL"), System.getenv("DEEPSEEK_MODEL"),
                        System.getProperty("deepseek.api.model"), DEFAULT_MODEL)
        );
    }

    /**
     * Explicit configuration; the client is treated as enabled regardless of environment.
     */
    public DeepSeekClient(String apiKey, String baseUrl, String model) {
        this(true, apiKey, baseUrl, model);
    }

    /**
     * Full constructor allowing an explicit enabled flag, used by
     * {@link #fromAdminSettings(AiApiSettings)} so the admin toggle bypasses env-var checks.
     */
    public DeepSeekClient(boolean enabled, String apiKey, String baseUrl, String model) {
        this.enabled = enabled;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.baseUrl = trimTrailingSlash(baseUrl != null && !baseUrl.trim().isEmpty()
                ? baseUrl.trim() : DEFAULT_BASE_URL);
        this.model = model != null && !model.trim().isEmpty() ? model.trim() : DEFAULT_MODEL;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Build a client strictly from admin-managed settings. When {@code settings} is null,
     * disabled or missing the key, the returned client reports {@link #isConfigured()} ==
     * false and refuses to send requests. The {@code provider} field is treated as a
     * free-form label and does not gate calls, so an admin can repoint Base URL / Model at
     * any OpenAI-compatible endpoint (DeepSeek, OpenAI, Moonshot, Ollama, ...).
     */
    public static DeepSeekClient fromAdminSettings(AiApiSettings settings) {
        if (settings == null) {
            return new DeepSeekClient(false, "", "", "");
        }
        return new DeepSeekClient(
                settings.isApiEnabled(),
                settings.getApiKey(),
                settings.getBaseUrl(),
                settings.getModel()
        );
    }

    /**
     * @return true if a non-blank API key is configured and the client is enabled
     */
    public boolean isConfigured() {
        return enabled && !apiKey.isEmpty();
    }

    private static boolean isEnabledFromEnv() {
        String enabled = firstNonBlank(System.getenv("TA_AI_ENABLED"));
        if (!enabled.isEmpty()
                && ("false".equalsIgnoreCase(enabled) || "0".equals(enabled) || "off".equalsIgnoreCase(enabled))) {
            return false;
        }
        return isProviderAccepted(firstNonBlank(System.getenv("TA_AI_PROVIDER")));
    }

    private static boolean isProviderAccepted(String provider) {
        if (provider == null) {
            return true;
        }
        String trimmed = provider.trim();
        return trimmed.isEmpty() || "deepseek".equalsIgnoreCase(trimmed);
    }

    private static String resolveApiKey() {
        return firstNonBlank(
                System.getenv("TA_AI_API_KEY"),
                System.getenv("DEEPSEEK_API_KEY"),
                System.getProperty("deepseek.api.key")
        );
    }

    /**
     * Single user message, optional system prompt.
     *
     * @throws IllegalStateException if not configured
     * @throws IOException          HTTP or API errors
     */
    public String chat(String systemPrompt, String userMessage) throws IOException {
        Objects.requireNonNull(userMessage, "userMessage");
        JsonArray messages = new JsonArray();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", systemPrompt.trim());
            messages.add(sys);
        }
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMessage);
        messages.add(user);
        return chatCompletions(messages);
    }

    /**
     * Full control over the messages array (e.g. multi-turn).
     */
    public String chatCompletions(JsonArray messages) throws IOException {
        if (!isConfigured()) {
            throw new IllegalStateException("DEEPSEEK_API_KEY is not set; cannot call DeepSeek API.");
        }
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("temperature", 0.3);

        String url = baseUrl + "/v1/chat/completions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("DeepSeek request interrupted", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("DeepSeek HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        if (root.has("error")) {
            throw new IOException("DeepSeek API error: " + root.get("error"));
        }
        JsonArray choices = root.getAsJsonArray("choices");
        if (choices == null || choices.size() == 0) {
            throw new IOException("DeepSeek empty choices: " + response.body());
        }
        JsonObject first = choices.get(0).getAsJsonObject();
        JsonObject msg = first.getAsJsonObject("message");
        if (msg == null || !msg.has("content")) {
            throw new IOException("DeepSeek unexpected response: " + response.body());
        }
        return msg.get("content").getAsString();
    }

    /**
     * Streaming counterpart of {@link #chat(String, String)}. Pushes each non-empty content
     * delta from the OpenAI-compatible SSE response to {@code onChunk}. Returns the
     * concatenation of all delivered chunks so callers can also persist or log the full text.
     * The consumer is not called for the final {@code [DONE]} sentinel.
     *
     * @throws IllegalStateException if not configured
     * @throws IOException          HTTP, parsing, or upstream API errors
     */
    public String chatStream(String systemPrompt, String userMessage, Consumer<String> onChunk) throws IOException {
        Objects.requireNonNull(userMessage, "userMessage");
        Objects.requireNonNull(onChunk, "onChunk");
        if (!isConfigured()) {
            throw new IllegalStateException("DEEPSEEK_API_KEY is not set; cannot call DeepSeek API.");
        }

        JsonArray messages = new JsonArray();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", systemPrompt.trim());
            messages.add(sys);
        }
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userMessage);
        messages.add(user);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        body.addProperty("temperature", 0.3);
        body.addProperty("stream", true);

        String url = baseUrl + "/v1/chat/completions";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("DeepSeek streaming request interrupted", e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("DeepSeek HTTP " + response.statusCode() + " on streaming request");
        }

        StringBuilder fullText = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (payload.isEmpty() || "[DONE]".equals(payload)) {
                    if ("[DONE]".equals(payload)) {
                        break;
                    }
                    continue;
                }
                String chunk = extractDeltaContent(payload);
                if (chunk != null && !chunk.isEmpty()) {
                    fullText.append(chunk);
                    onChunk.accept(chunk);
                }
            }
        }
        return fullText.toString();
    }

    private static String extractDeltaContent(String jsonPayload) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(jsonPayload).getAsJsonObject();
            if (root.has("error")) {
                throw new IOException("DeepSeek API error: " + root.get("error"));
            }
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                return null;
            }
            JsonObject first = choices.get(0).getAsJsonObject();
            if (!first.has("delta")) {
                return null;
            }
            JsonObject delta = first.getAsJsonObject("delta");
            if (delta == null || !delta.has("content") || delta.get("content").isJsonNull()) {
                return null;
            }
            return delta.get("content").getAsString();
        } catch (IllegalStateException | com.google.gson.JsonSyntaxException e) {
            throw new IOException("Failed to parse DeepSeek SSE payload: " + jsonPayload, e);
        }
    }

    private static String trimTrailingSlash(String u) {
        if (u == null || u.length() <= 1) {
            return u;
        }
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return "";
    }
}
