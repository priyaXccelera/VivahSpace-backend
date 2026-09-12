package com.example.userservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET =
      "Test-Secret-Key-For-JWT-Signing-Must-Be-At-Least-32-Bytes-Long!";
  private static final long EXPIRATION_MS = 60_000L;

  private JwtService jwtService;
  private User user;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService(SECRET, EXPIRATION_MS);

    user = new User();
    user.setId(42L);
    user.setFullName("Jane Doe");
    user.setEmail("jane@example.com");
    user.setPasswordHash("hashed-password");
    user.setRole(Role.ADMIN);
    user.setActive(true);
  }

  @Test
  void getExpirationMs_returnsConfiguredValue() {
    assertThat(jwtService.getExpirationMs()).isEqualTo(EXPIRATION_MS);
  }

  @Test
  void generateToken_thenExtractEmail_returnsUserEmail() {
    String token = jwtService.generateToken(user);

    assertThat(jwtService.extractEmail(token)).isEqualTo("jane@example.com");
  }

  @Test
  void generateToken_thenExtractUserId_returnsUserId() {
    String token = jwtService.generateToken(user);

    assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
  }

  @Test
  void generateToken_thenExtractRole_returnsUserRole() {
    String token = jwtService.generateToken(user);

    assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
  }

  @Test
  void isTokenValid_withMatchingEmailAndUnexpiredToken_returnsTrue() {
    String token = jwtService.generateToken(user);

    assertThat(jwtService.isTokenValid(token, "jane@example.com")).isTrue();
    assertThat(jwtService.isTokenValid(token, "JANE@EXAMPLE.COM")).isTrue();
  }

  @Test
  void isTokenValid_withMismatchedEmail_returnsFalse() {
    String token = jwtService.generateToken(user);

    assertThat(jwtService.isTokenValid(token, "someone-else@example.com")).isFalse();
  }

  @Test
  void isTokenValid_withMalformedToken_returnsFalse() {
    assertThat(jwtService.isTokenValid("not-a-real-token", "jane@example.com")).isFalse();
  }

  @Test
  void isTokenParsable_withValidToken_returnsTrue() {
    String token = jwtService.generateToken(user);

    assertThat(jwtService.isTokenParsable(token)).isTrue();
  }

  @Test
  void isTokenParsable_withMalformedToken_returnsFalse() {
    assertThat(jwtService.isTokenParsable("garbage.token.value")).isFalse();
  }

  @Test
  void isTokenParsable_withTokenSignedByDifferentKey_returnsFalse() {
    JwtService otherJwtService =
        new JwtService(
            "A-Completely-Different-Signing-Key-That-Is-Also-Long-Enough!!", EXPIRATION_MS);
    String token = otherJwtService.generateToken(user);

    assertThat(jwtService.isTokenParsable(token)).isFalse();
    assertThat(jwtService.isTokenValid(token, "jane@example.com")).isFalse();
  }

  @Test
  void expiredToken_isNeitherParsableNorValid() {
    JwtService expiringJwtService = new JwtService(SECRET, -10_000L);
    String expiredToken = expiringJwtService.generateToken(user);

    assertThat(expiringJwtService.isTokenParsable(expiredToken)).isFalse();
    assertThat(expiringJwtService.isTokenValid(expiredToken, "jane@example.com")).isFalse();
  }
}
