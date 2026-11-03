package com.myai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings. The secret must come from the environment (APP_JWT_SECRET) and be at least 32 bytes.
 * If it is blank an ephemeral random key is generated at startup (tokens then die on restart),
 * which is acceptable for local development only.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long ttlSeconds) {
}
