package bupt.ta.util;

import javax.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Issues and validates session-bound CSRF tokens.
 */
public final class CsrfTokenManager {

    public static final String SESSION_ATTRIBUTE = "csrfToken";
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfTokenManager() {
    }

    /**
     * @param session HTTP session
     * @return existing CSRF token or a newly generated one stored on the session
     */
    public static String getOrCreateToken(HttpSession session) {
        Object existing = session.getAttribute(SESSION_ATTRIBUTE);
        if (existing instanceof String && !((String) existing).trim().isEmpty()) {
            return (String) existing;
        }
        String token = generateToken();
        session.setAttribute(SESSION_ATTRIBUTE, token);
        return token;
    }

    /**
     * @param expectedToken token from the session
     * @param providedToken token from the request
     * @return {@code true} when both are non-null and equal (constant-time)
     */
    public static boolean isValid(String expectedToken, String providedToken) {
        if (expectedToken == null || providedToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
