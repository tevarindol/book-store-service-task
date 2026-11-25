package dev.is_a.tevarindol.bookstore.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
    String secret,
    String issuer,
    String cookieName,
    Duration accessTokenTtl,
    boolean secureCookie) {}
