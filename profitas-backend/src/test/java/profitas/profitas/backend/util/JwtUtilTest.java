package profitas.profitas.backend.util;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    @Test
    void rejectsShortOrMissingSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("too-short", 1000));
        assertThrows(IllegalStateException.class, () -> new JwtUtil(null, 1000));
    }

    @Test
    void roundTripsEmail() {
        JwtUtil jwt = new JwtUtil(SECRET, 60_000);
        assertEquals("a@b.com", jwt.extractEmail(jwt.generateToken("a@b.com")));
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        String token = new JwtUtil("another-secret-another-secret-123456", 60_000).generateToken("a@b.com");
        assertThrows(JwtException.class, () -> new JwtUtil(SECRET, 60_000).extractEmail(token));
    }

    @Test
    void rejectsExpiredToken() {
        JwtUtil jwt = new JwtUtil(SECRET, -1000);
        String token = jwt.generateToken("a@b.com");
        assertThrows(JwtException.class, () -> jwt.extractEmail(token));
    }
}
