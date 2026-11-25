package dev.is_a.tevarindol.bookstore.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtDecoder decoder;
  private final UserDetailsService users;
  private final JwtCookieService cookies;
  private final JwtProperties properties;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Optional<String> token =
        Optional.ofNullable(request.getCookies()).stream()
            .flatMap(Arrays::stream)
            .filter(cookie -> cookie.getName().equals(properties.cookieName()))
            .map(Cookie::getValue)
            .findFirst();

    if (token.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
      try {
        String subject = decoder.decode(token.get()).getSubject();
        UserDetails user = users.loadUserByUsername(subject);
        if (!user.isEnabled()) {
          cookies.clear(response);
        } else {
          SecurityContext context = SecurityContextHolder.createEmptyContext();
          context.setAuthentication(
              UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
          SecurityContextHolder.setContext(context);
        }
      } catch (JwtException | UsernameNotFoundException exception) {
        SecurityContextHolder.clearContext();
        cookies.clear(response);
      }
    }

    filterChain.doFilter(request, response);
  }
}
