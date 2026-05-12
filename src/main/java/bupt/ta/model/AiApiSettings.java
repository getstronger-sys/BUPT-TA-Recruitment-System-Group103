package bupt.ta.model;

/**
 * Admin-managed LLM (DeepSeek-compatible) API settings.
 * Persisted to {@code data/ai-api-settings.json}; consumed by {@link bupt.ta.llm.DeepSeekClient}
 * via {@link bupt.ta.llm.DeepSeekClient#fromAdminSettings(AiApiSettings)}.
 *
 * <p>The API key is stored as plain text in the data directory (sufficient for a course
 * project; in production, use a secrets manager and restrict file system permissions).</p>
 */
public class AiApiSettings {

    private boolean apiEnabled = true;
    private String provider = "deepseek";
    private String baseUrl = "";
    private String model = "";
    private String apiKey = "";

    public boolean isApiEnabled() {
        return apiEnabled;
    }

    public void setApiEnabled(boolean apiEnabled) {
        this.apiEnabled = apiEnabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider != null ? provider.trim() : "";
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "";
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model != null ? model.trim() : "";
    }

    /**
     * Plain-text API key. Empty string means "no key configured".
     */
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey : "";
    }

    /**
     * @return true if a real call is allowed: enabled and key present. The {@code provider}
     *         field is treated as a free-form label and is not validated here, because the
     *         underlying HTTP client speaks the OpenAI-compatible protocol that several
     *         vendors (DeepSeek, OpenAI, Moonshot, Ollama, ...) already support.
     */
    public boolean isEffectivelyConfigured() {
        if (!apiEnabled) {
            return false;
        }
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
