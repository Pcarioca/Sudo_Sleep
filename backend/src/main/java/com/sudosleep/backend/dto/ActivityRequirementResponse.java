package com.sudosleep.backend.dto;

import com.sudosleep.backend.entity.ActivityType;

public record ActivityRequirementResponse(
        Long id,
        Long subjectId,
        ActivityType activityType,
        String customTypeName,
        boolean scheduled) {
}
