package com.myai.config;

import org.springframework.security.oauth2.jwt.Jwt;

/** Resolves the authenticated user id from the JWT subject. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public static Long idOrNull(Jwt jwt) {
        return jwt == null ? null : id(jwt);
    }
}
