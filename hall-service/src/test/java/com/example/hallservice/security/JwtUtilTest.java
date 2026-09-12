package com.example.hallservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

  private static final String SECRET = "test-secret-key-with-at-least-32-bytes-length!!";

  private JwtUtil jwtUtil;
  private SecretKey signingKey;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
  }

  private String buildToken(String subject, Date expiration) {
    return Jwts.builder()
        .setSubject(subject)
        .setExpiration(expiration)
        .signWith(signingKey)
        .compact();
  }

  @Test
  void validateToken_validToken_returnsTrue() {
    String token = buildToken("user-1", new Date(System.currentTimeMillis() + 60_000));

    assertThat(jwtUtil.validateToken(token)).isTrue();
  }

  @Test
  void validateToken_expiredToken_returnsFalse() {
    String token = buildToken("user-1", new Date(System.currentTimeMillis() - 60_000));

    assertThat(jwtUtil.validateToken(token)).isFalse();
  }

  @Test
  void validateToken_malformedToken_returnsFalse() {
    assertThat(jwtUtil.validateToken("not-a-valid-token")).isFalse();
  }

  @Test
  void validateToken_wrongSigningKey_returnsFalse() {
    SecretKey otherKey =
        Keys.hmacShaKeyFor(
            "a-completely-different-secret-key-32bytes!!".getBytes(StandardCharsets.UTF_8));
    String token =
        Jwts.builder()
            .setSubject("user-1")
            .setExpiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(otherKey)
            .compact();

    assertThat(jwtUtil.validateToken(token)).isFalse();
  }

  @Test
  void extractSubject_returnsExpectedSubject() {
    String token = buildToken("user-42", new Date(System.currentTimeMillis() + 60_000));

    assertThat(jwtUtil.extractSubject(token)).isEqualTo("user-42");
  }
}
