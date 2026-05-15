package bupt.ta.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link PasswordHasher}. */
public class PasswordHasherTest {

    @Test
    public void hashProducesRecognizedFormatAndMatchesOriginalPassword() {
        String hashed = PasswordHasher.hash("test123");

        assertTrue(PasswordHasher.isHashed(hashed));
        assertTrue(PasswordHasher.matches("test123", hashed));
        assertFalse(PasswordHasher.matches("wrong-pass", hashed));
    }

    @Test
    public void matchesSupportsLegacyPlaintextPasswords() {
        assertTrue(PasswordHasher.matches("ta123", "ta123"));
        assertFalse(PasswordHasher.isHashed("ta123"));
        assertFalse(PasswordHasher.matches("other", "ta123"));
    }

    @Test
    public void malformedStoredHashDoesNotAuthenticate() {
        assertFalse(PasswordHasher.matches("test123", "pbkdf2$broken"));
    }
}
