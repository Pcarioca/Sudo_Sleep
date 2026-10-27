package com.sudosleep.backend.mapper;

import com.sudosleep.backend.dto.ActivityRequirementResponse;
import com.sudosleep.backend.dto.UnscheduledRequirementResponse;
import com.sudosleep.backend.entity.ActivityRequirement;
import com.sudosleep.backend.entity.Subject;
import org.springframework.stereotype.Component;

@Component
public class ActivityRequirementMapper {

    public ActivityRequirementResponse toResponse(ActivityRequirement requirement, boolean scheduled) {
        return new ActivityRequirementResponse(
                requirement.getId(),
                requirement.getSubject().getId(),
                requirement.getActivityType(),
                requirement.getCustomTypeName(),
                scheduled);
    }

    public UnscheduledRequirementResponse toUnscheduledResponse(ActivityRequirement requirement) {
        Subject subject = requirement.getSubject();
        return new UnscheduledRequirementResponse(
                requirement.getId(),
                subject.getId(),
                subject.getName(),
                subject.getColorHex(),
                requirement.getActivityType(),
                requirement.getCustomTypeName());
    }
}
