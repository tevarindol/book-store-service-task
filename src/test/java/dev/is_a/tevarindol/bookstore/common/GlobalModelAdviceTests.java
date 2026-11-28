package dev.is_a.tevarindol.bookstore.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class GlobalModelAdviceTests {
  private final GlobalModelAdvice advice = new GlobalModelAdvice();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("authenticationStates")
  void exposesAuthenticationAndRole(
      String scenario, Authentication authentication, boolean signedIn, boolean employee) {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    assertThat(advice.signedIn()).isEqualTo(signedIn);
    assertThat(advice.employee()).isEqualTo(employee);
  }

  private static Stream<Arguments> authenticationStates() {
    String email = "reader@example.com";
    return Stream.of(
        arguments("no authentication", null, false, false),
        arguments(
            "unauthenticated token",
            UsernamePasswordAuthenticationToken.unauthenticated(email, null),
            false,
            false),
        arguments(
            "anonymous user",
            new AnonymousAuthenticationToken(
                "test-key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))),
            false,
            false),
        arguments(
            "customer",
            UsernamePasswordAuthenticationToken.authenticated(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))),
            true,
            false),
        arguments(
            "employee",
            UsernamePasswordAuthenticationToken.authenticated(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))),
            true,
            true));
  }
}
