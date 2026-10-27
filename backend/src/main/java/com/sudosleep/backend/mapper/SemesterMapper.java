package com.sudosleep.backend.mapper;

import com.sudosleep.backend.dto.SemesterResponse;
import com.sudosleep.backend.entity.Semester;
import org.springframework.stereotype.Component;

@Component
public class SemesterMapper {

    public SemesterResponse toResponse(Semester semester, Integer currentWeek) {
        return new SemesterResponse(
                semester.getId(),
                semester.getUser().getId(),
                semester.getName(),
                semester.getStartDate(),
                semester.getWeeksCount(),
                currentWeek);
    }
}
