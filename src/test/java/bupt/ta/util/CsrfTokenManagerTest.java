package bupt.ta.util;

import org.junit.Test;

import javax.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Unit tests for {@link CsrfTokenManager}. */
public class CsrfTokenManagerTest {

    @Test
    public void getOrCreateTokenReusesExistingSessionToken() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CsrfTokenManager.SESSION_ATTRIBUTE, "existing-token");
        HttpSession session = mapBackedSession(attributes);

        String token = CsrfTokenManager.getOrCreateToken(session);

        assertSame("existing-token", token);
    }

    @Test
    public void getOrCreateTokenGeneratesTokenWhenMissing() {
        Map<String, Object> attributes = new HashMap<>();
        HttpSession session = mapBackedSession(attributes);

        String token = CsrfTokenManager.getOrCreateToken(session);

        assertNotNull(token);
        assertTrue(token.length() >= 32);
        assertSame(token, attributes.get(CsrfTokenManager.SESSION_ATTRIBUTE));
    }

    @Test
    public void isValidRequiresExactTokenMatch() {
        assertTrue(CsrfTokenManager.isValid("token-123", "token-123"));
        assertFalse(CsrfTokenManager.isValid("token-123", "token-456"));
        assertFalse(CsrfTokenManager.isValid("token-123", null));
        assertFalse(CsrfTokenManager.isValid(null, "token-123"));
    }

    private static HttpSession mapBackedSession(Map<String, Object> attributes) {
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getAttribute".equals(name)) {
                        return attributes.get(args[0]);
                    }
                    if ("setAttribute".equals(name)) {
                        attributes.put((String) args[0], args[1]);
                        return null;
                    }
                    if ("removeAttribute".equals(name)) {
                        attributes.remove(args[0]);
                        return null;
                    }
                    if ("toString".equals(name)) {
                        return "MapBackedSession";
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    return null;
                }
        );
    }
}
