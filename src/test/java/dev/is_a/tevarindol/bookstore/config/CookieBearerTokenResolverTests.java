package dev.is_a.tevarindol.bookstore.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CookieBearerTokenResolverTests {
  private static final String COOKIE_NAME = "TEST_AUTH";

  private final CookieBearerTokenResolver resolver =
      new CookieBearerTokenResolver(
          new JwtProperties(
              "unused", "bookstore-test", COOKIE_NAME, Duration.ofMinutes(15), false));
  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @Test
  void returnsNullWhenRequestHasNoCookies() {
    assertThat(resolver.resolve(request)).isNull();
  }

  @Test
  void returnsNullWhenNoCookieMatchesConfiguredName() {
    request.setCookies(new Cookie("other", "unrelated-value"));

    assertThat(resolver.resolve(request)).isNull();
  }

  @Test
  void returnsFirstCookieMatchingConfiguredName() {
    request.setCookies(
        new Cookie("other", "unrelated-value"),
        new Cookie(COOKIE_NAME, "first-token"),
        new Cookie(COOKIE_NAME, "second-token"));

    assertThat(resolver.resolve(request)).isEqualTo("first-token");
  }
}
