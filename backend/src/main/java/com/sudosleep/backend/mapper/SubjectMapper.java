package com.sudosleep.backend.mapper;

import com.sudosleep.backend.dto.ActivityRequirementResponse;
import com.sudosleep.backend.dto.SubjectResponse;
import com.sudosleep.backend.entity.Subject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SubjectMapper {

    public SubjectResponse toResponse(Subject subject,
                                      List<ActivityRequirementResponse> requirements,
                                      int gradeCount,
                                      BigDecimal arithmeticAverage,
                                      BigDecimal weightedAverage) {
        return new SubjectResponse(
                subject.getId(),
                subject.getSemester().getId(),
                subject.getName(),
                subject.getColorHex(),
                requirements,
                gradeCount,
                arithmeticAverage,
                weightedAverage);
    }
}
