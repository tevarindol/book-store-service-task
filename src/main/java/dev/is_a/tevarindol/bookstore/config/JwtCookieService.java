package dev.is_a.tevarindol.bookstore.config;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtCookieService {
  private final JwtProperties properties;

  public void writeToken(HttpServletResponse response, String token) {
    response.addHeader(
        HttpHeaders.SET_COOKIE, cookie(token, properties.accessTokenTtl()).toString());
  }

  public void clear(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
  }

  private ResponseCookie cookie(String value, Duration maxAge) {
    return ResponseCookie.from(properties.cookieName(), value)
        .httpOnly(true)
        .secure(properties.secureCookie())
        .sameSite("Lax")
        .path("/")
        .maxAge(maxAge)
        .build();
  }
}
