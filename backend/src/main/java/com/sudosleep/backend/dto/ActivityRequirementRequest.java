package com.sudosleep.backend.dto;

import com.sudosleep.backend.entity.ActivityType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActivityRequirementRequest(
        @NotNull ActivityType activityType,
        @Size(max = 100) String customTypeName) {
}
