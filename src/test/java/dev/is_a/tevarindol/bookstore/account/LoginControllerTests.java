package dev.is_a.tevarindol.bookstore.account;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import dev.is_a.tevarindol.bookstore.config.JwtCookieService;
import dev.is_a.tevarindol.bookstore.config.JwtProperties;
import dev.is_a.tevarindol.bookstore.config.JwtTokenService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class LoginControllerTests {
  private static final String EMAIL = "reader@example.com";
  private static final String PASSWORD = "test-password";
  private static final String AUTH_COOKIE = "BOOKSTORE_AUTH";

  private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
  private final JwtTokenService tokens = mock(JwtTokenService.class);
  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(
              new LoginController(
                  authenticationManager,
                  tokens,
                  new JwtCookieService(
                      new JwtProperties(
                          "unused", "bookstore-test", AUTH_COOKIE, Duration.ofMinutes(15), false))))
          .build();

  @Test
  void successfulLoginWritesJwtCookieAndRedirectsToBooks() throws Exception {
    Authentication authenticated = mock(Authentication.class);
    when(authenticationManager.authenticate(unauthenticated(EMAIL, PASSWORD)))
        .thenReturn(authenticated);
    when(tokens.issue(authenticated)).thenReturn("issued-token");

    mvc.perform(post("/login").param("username", EMAIL).param("password", PASSWORD))
        .andExpect(redirectedUrl("/books"))
        .andExpect(cookie().value(AUTH_COOKIE, "issued-token"))
        .andExpect(cookie().httpOnly(AUTH_COOKIE, true))
        .andExpect(cookie().maxAge(AUTH_COOKIE, 900));
  }

  @Test
  void invalidCredentialsClearAuthCookieAndRedirectToLoginError() throws Exception {
    when(authenticationManager.authenticate(unauthenticated(EMAIL, PASSWORD)))
        .thenThrow(new BadCredentialsException("Invalid credentials"));

    mvc.perform(post("/login").param("username", EMAIL).param("password", PASSWORD))
        .andExpect(redirectedUrl("/login?error"))
        .andExpect(cookie().value(AUTH_COOKIE, ""))
        .andExpect(cookie().maxAge(AUTH_COOKIE, 0));

    verifyNoInteractions(tokens);
  }

  @ParameterizedTest
  @CsvSource({"' ', test-password", "reader@example.com, ' '"})
  void blankFieldRedirectsWithoutAuthenticatingOrChangingCookie(String username, String password)
      throws Exception {
    mvc.perform(post("/login").param("username", username).param("password", password))
        .andExpect(redirectedUrl("/login?error"))
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

    verifyNoInteractions(authenticationManager, tokens);
  }
}
