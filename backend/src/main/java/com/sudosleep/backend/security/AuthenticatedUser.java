package com.sudosleep.backend.security;

/** Principal placed into the Spring SecurityContext after JWT validation. */
public record AuthenticatedUser(Long id, String email) {
}
