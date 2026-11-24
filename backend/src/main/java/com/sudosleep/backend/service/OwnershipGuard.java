package com.sudosleep.backend.service;

import com.sudosleep.backend.exception.ForbiddenException;

import java.util.Objects;

/** Central ownership check ensuring every API operation stays strictly within the authenticated user's data. */
final class OwnershipGuard {

    private OwnershipGuard() {
    }

    static void requireOwner(Long ownerUserId, Long currentUserId) {
        if (currentUserId == null || !Objects.equals(ownerUserId, currentUserId)) {
            throw new ForbiddenException("You do not have permission to access this resource");
        }
    }
}
