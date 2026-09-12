package com.example.vendorservice.security;

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

  private static final String SECRET =
      "test-secret-key-for-jwt-signing-must-be-at-least-32-bytes-long!";

  private JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil();
    ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
  }

  private String buildToken(String subject, String secret, long expiryMillisFromNow) {
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    Date now = new Date();
    return Jwts.builder()
        .setSubject(subject)
        .setIssuedAt(now)
        .setExpiration(new Date(now.getTime() + expiryMillisFromNow))
        .signWith(key)
        .compact();
  }

  @Test
  void validateToken_validToken_returnsTrue() {
    String token = buildToken("vendor-user", SECRET, 60_000);

    assertThat(jwtUtil.validateToken(token)).isTrue();
  }

  @Test
  void validateToken_expiredToken_returnsFalse() {
    String token = buildToken("vendor-user", SECRET, -60_000);

    assertThat(jwtUtil.validateToken(token)).isFalse();
  }

  @Test
  void validateToken_wrongSigningKey_returnsFalse() {
    String token =
        buildToken("vendor-user", "a-completely-different-secret-key-of-32-bytes!", 60_000);

    assertThat(jwtUtil.validateToken(token)).isFalse();
  }

  @Test
  void validateToken_malformedToken_returnsFalse() {
    assertThat(jwtUtil.validateToken("not-a-valid-jwt")).isFalse();
  }

  @Test
  void extractSubject_validToken_returnsSubject() {
    String token = buildToken("vendor-admin", SECRET, 60_000);

    assertThat(jwtUtil.extractSubject(token)).isEqualTo("vendor-admin");
  }
}
