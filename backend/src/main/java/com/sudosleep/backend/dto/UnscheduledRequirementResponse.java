package com.sudosleep.backend.dto;

import com.sudosleep.backend.entity.ActivityType;

/** One row of the "unscheduled activities" banner. */
public record UnscheduledRequirementResponse(
        Long requirementId,
        Long subjectId,
        String subjectName,
        String colorHex,
        ActivityType activityType,
        String customTypeName) {
}
