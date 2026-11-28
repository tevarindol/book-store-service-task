package dev.is_a.tevarindol.bookstore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class JwtTokenServiceTests {
  @Test
  void issuesTokenWithConfiguredIssuerAndLifetimeForAuthenticatedUser() {
    String email = "reader@example.com";
    String issuer = "bookstore-test";
    Duration ttl = Duration.ofMinutes(7);
    JwtEncoder encoder = mock(JwtEncoder.class);
    Jwt encoded = mock(Jwt.class);
    when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(encoded);
    when(encoded.getTokenValue()).thenReturn("encoded-token");
    var service =
        new JwtTokenService(encoder, new JwtProperties("unused", issuer, "TEST_AUTH", ttl, false));
    var authentication = UsernamePasswordAuthenticationToken.authenticated(email, null, List.of());

    assertThat(service.issue(authentication)).isEqualTo("encoded-token");

    var parameters = ArgumentCaptor.forClass(JwtEncoderParameters.class);
    verify(encoder).encode(parameters.capture());
    var encodedParameters = parameters.getValue();
    assertThat(encodedParameters.getJwsHeader().getAlgorithm()).isEqualTo(MacAlgorithm.HS256);
    var claims = encodedParameters.getClaims();
    assertThat(claims.getSubject()).isEqualTo(email);
    assertThat(claims.getClaimAsString("iss")).isEqualTo(issuer);
    assertThat(Duration.between(claims.getIssuedAt(), claims.getExpiresAt())).isEqualTo(ttl);
  }
}
