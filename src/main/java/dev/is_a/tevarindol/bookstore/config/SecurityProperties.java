package dev.is_a.tevarindol.bookstore.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    @Min(4) @Max(31) int bcryptStrength, @Min(0) @Max(4) int passwordMinScore) {}
