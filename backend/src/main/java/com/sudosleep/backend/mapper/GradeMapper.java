package com.sudosleep.backend.mapper;

import com.sudosleep.backend.dto.GradeResponse;
import com.sudosleep.backend.entity.Grade;
import org.springframework.stereotype.Component;

@Component
public class GradeMapper {

    public GradeResponse toResponse(Grade grade) {
        return new GradeResponse(
                grade.getId(),
                grade.getSubject().getId(),
                grade.getScore(),
                grade.getWeight());
    }
}
