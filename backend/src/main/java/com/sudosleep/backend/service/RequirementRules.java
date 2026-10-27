package com.sudosleep.backend.service;

import com.sudosleep.backend.dto.ActivityRequirementRequest;
import com.sudosleep.backend.entity.ActivityType;
import com.sudosleep.backend.exception.BadRequestException;

/** Validation shared by everything that creates or edits an activity requirement. */
final class RequirementRules {

    private RequirementRules() {
    }

    /**
     * {@code customTypeName} is required for {@code CUSTOM} and forbidden otherwise
     * (mirrors the CHECK constraint on the table).
     *
     * @return the name to store: trimmed for CUSTOM, {@code null} for the other types
     */
    static String customNameFor(ActivityRequirementRequest request) {
        String name = request.customTypeName() == null ? null : request.customTypeName().trim();
        boolean blank = name == null || name.isEmpty();
        if (request.activityType() == ActivityType.CUSTOM) {
            if (blank) {
                throw new BadRequestException("customTypeName is required when activityType is CUSTOM");
            }
            return name;
        }
        if (!blank) {
            throw new BadRequestException("customTypeName is only allowed when activityType is CUSTOM");
        }
        return null;
    }
}
