package bupt.ta.llm;

import bupt.ta.model.AiApiSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeepSeekClientEnvFallbackTest {

    private static final String[] PROPS = {
            "TA_AI_ENABLED", "TA_AI_API_KEY", "TA_AI_BASE_URL", "TA_AI_MODEL", "TA_AI_PROVIDER"
    };

    private final String[] saved = new String[PROPS.length];

    @Before
    public void saveProps() {
        for (int i = 0; i < PROPS.length; i++) {
            saved[i] = System.getProperty(PROPS[i]);
        }
    }

    @After
    public void restoreProps() {
        for (int i = 0; i < PROPS.length; i++) {
            if (saved[i] == null) {
                System.clearProperty(PROPS[i]);
            } else {
                System.setProperty(PROPS[i], saved[i]);
            }
        }
    }

    @Test
    public void usesAdminSettingsWhenKeyPresent() {
        clearProps();
        AiApiSettings admin = new AiApiSettings();
        admin.setApiEnabled(true);
        admin.setApiKey("sk-admin");
        admin.setBaseUrl("https://api.example.com");
        admin.setModel("admin-model");

        assertTrue(DeepSeekClient.fromRuntimeSettings(admin).isConfigured());
        assertEquals("sk-admin", DeepSeekClient.mergeWithEnvFallback(admin).getApiKey());
    }

    @Test
    public void fallsBackToEnvPropertiesWhenAdminKeyMissing() {
        clearProps();
        System.setProperty("TA_AI_ENABLED", "true");
        System.setProperty("TA_AI_API_KEY", "sk-from-env");
        System.setProperty("TA_AI_BASE_URL", "https://api.deepseek.com");
        System.setProperty("TA_AI_MODEL", "deepseek-chat");

        AiApiSettings admin = new AiApiSettings();
        assertFalse(admin.isEffectivelyConfigured());
        assertTrue(DeepSeekClient.isRuntimeConfigured(admin));

        AiApiSettings merged = DeepSeekClient.mergeWithEnvFallback(admin);
        assertEquals("sk-from-env", merged.getApiKey());
        assertEquals("deepseek-chat", merged.getModel());
    }

    @Test
    public void remainsUnconfiguredWhenNeitherAdminNorEnv() {
        clearProps();
        assertFalse(DeepSeekClient.isRuntimeConfigured(new AiApiSettings()));
    }

    private static void clearProps() {
        for (String prop : PROPS) {
            System.clearProperty(prop);
        }
    }
}
