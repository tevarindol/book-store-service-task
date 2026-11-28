package dev.is_a.tevarindol.bookstore.config;

import dev.is_a.tevarindol.bookstore.account.AccountRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtCookieService jwtCookies,
      JwtProperties jwtProperties,
      AccountJwtAuthenticationConverter jwtAuthenticationConverter,
      CookieBearerTokenResolver cookieBearerTokenResolver)
      throws Exception {
    CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
    csrfTokens.setCookiePath("/");
    csrfTokens.setCookieCustomizer(
        cookie -> cookie.secure(jwtProperties.secureCookie()).sameSite("Lax"));
    return http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/",
                        "/books/**",
                        "/login",
                        "/register",
                        "/css/**",
                        "/images/**",
                        "/error",
                        "/error/**")
                    .permitAll()
                    .requestMatchers("/employee/**")
                    .hasRole("EMPLOYEE")
                    .requestMatchers("/basket/**", "/checkout", "/orders/**")
                    .hasRole("CUSTOMER")
                    .requestMatchers("/profile/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(csrfTokens)
                    .withObjectPostProcessor(
                        new ObjectPostProcessor<CsrfFilter>() {
                          @Override
                          public <O extends CsrfFilter> O postProcess(O filter) {
                            // Cookie-based bearer authentication still needs CSRF protection.
                            filter.setRequireCsrfProtectionMatcher(CsrfFilter.DEFAULT_CSRF_MATCHER);
                            return filter;
                          }
                        }))
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .bearerTokenResolver(cookieBearerTokenResolver)
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                    .authenticationEntryPoint(
                        (request, response, exception) -> {
                          jwtCookies.clear(response);
                          response.sendRedirect("/login");
                        }))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(cache -> cache.requestCache(new NullRequestCache()))
        .logout(
            logout ->
                logout
                    .logoutSuccessUrl("/books?logout")
                    .addLogoutHandler(
                        (request, response, authentication) -> jwtCookies.clear(response)))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (request, response, exception) -> response.sendRedirect("/login"))
                    .accessDeniedPage("/error/403"))
        .build();
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  UserDetailsService userDetailsService(AccountRepository accounts) {
    return email ->
        accounts
            .findByEmailIgnoreCase(email)
            .map(
                account ->
                    User.withUsername(account.getEmail())
                        .password(account.getPasswordHash())
                        .roles(account.getRole().name())
                        .disabled(account.isBlocked())
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("error.accountNotFound"));
  }

  @Bean
  PasswordEncoder passwordEncoder(SecurityProperties properties) {
    return new BCryptPasswordEncoder(properties.bcryptStrength());
  }
}
