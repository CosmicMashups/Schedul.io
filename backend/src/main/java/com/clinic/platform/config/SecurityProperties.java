package com.clinic.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Correction from Milestone 1: {@code @Value("${clinic.security.public-paths}")} into a
 * List&lt;String&gt; does not reliably bind a YAML sequence — Spring's @Value resolves a
 * single property expression, not a relaxed-bound collection. @ConfigurationProperties does
 * support list binding correctly and is the standard approach for structured config like this.
 */
@Validated
@ConfigurationProperties(prefix = "clinic.security")
public class SecurityProperties {

    private List<String> publicPaths = List.of();

    public List<String> getPublicPaths() {
        return publicPaths;
    }

    public void setPublicPaths(List<String> publicPaths) {
        this.publicPaths = publicPaths;
    }
}
