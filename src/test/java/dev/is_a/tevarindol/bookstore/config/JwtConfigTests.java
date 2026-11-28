package dev.is_a.tevarindol.bookstore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

class JwtConfigTests {
  private static final String SECRET = "я".repeat(16);
  private static final String ISSUER = "bookstore-test";
  private static final String SUBJECT = "reader@example.com";

  private final JwtConfig config = new JwtConfig();
  private final JwtProperties properties = properties(SECRET, ISSUER);
  private final SecretKey key = config.jwtSecretKey(properties);
  private final JwtEncoder encoder = config.jwtEncoder(key);
  private final JwtDecoder decoder = config.jwtDecoder(key, properties);
  private final UsernamePasswordAuthenticationToken authentication =
      UsernamePasswordAuthenticationToken.authenticated(SUBJECT, null, List.of());

  @Test
  void rejectsSecretShorterThan32Utf8Bytes() {
    JwtProperties shortSecret = properties("я".repeat(15) + "a", ISSUER);

    assertThatThrownBy(() -> config.jwtSecretKey(shortSecret))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("JWT_SECRET must contain at least 32 bytes for HS256");
  }

  @Test
  void signsAndDecodesTokenWithA32ByteSecret() {
    JwtTokenService service = new JwtTokenService(encoder, properties);

    Jwt decoded = decoder.decode(service.issue(authentication));

    assertThat(decoded.getHeaders()).containsEntry("alg", "HS256");
    assertThat(decoded.getSubject()).isEqualTo(SUBJECT);
    assertThat(decoded.getClaimAsString("iss")).isEqualTo(ISSUER);
  }

  @Test
  void rejectsTokenSignedWithSameKeyButDifferentIssuer() {
    JwtTokenService service = new JwtTokenService(encoder, properties(SECRET, "other-issuer"));
    String token = service.issue(authentication);

    assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtValidationException.class);
  }

  private static JwtProperties properties(String secret, String issuer) {
    return new JwtProperties(secret, issuer, "TEST_AUTH", Duration.ofMinutes(15), false);
  }
}
