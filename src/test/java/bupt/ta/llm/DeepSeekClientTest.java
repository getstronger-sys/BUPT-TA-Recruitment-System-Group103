package bupt.ta.llm;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link DeepSeekClient} configuration helpers. */
public class DeepSeekClientTest {

    @Test
    public void unconfiguredClientHasNoKey() {
        DeepSeekClient c = new DeepSeekClient("", DeepSeekClient.DEFAULT_BASE_URL, DeepSeekClient.DEFAULT_MODEL);
        assertFalse(c.isConfigured());
    }

    @Test
    public void configuredWhenKeyPresent() {
        DeepSeekClient c = new DeepSeekClient("sk-test", DeepSeekClient.DEFAULT_BASE_URL, DeepSeekClient.DEFAULT_MODEL);
        assertTrue(c.isConfigured());
    }
}
