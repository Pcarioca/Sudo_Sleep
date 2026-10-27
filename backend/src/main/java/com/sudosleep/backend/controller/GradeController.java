package com.sudosleep.backend.controller;

import com.sudosleep.backend.dto.GradeRequest;
import com.sudosleep.backend.dto.GradeResponse;
import com.sudosleep.backend.dto.GradeSummaryResponse;
import com.sudosleep.backend.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Grades")
public class GradeController {

    private final GradeService service;

    public GradeController(GradeService service) {
        this.service = service;
    }

    @GetMapping("/subjects/{subjectId}/grades")
    @Operation(summary = "List the grades of a subject")
    public List<GradeResponse> list(@PathVariable("subjectId") Long subjectId) {
        return service.listForSubject(subjectId);
    }

    @GetMapping("/subjects/{subjectId}/grades/summary")
    @Operation(summary = "Arithmetic and weighted average of a subject")
    public GradeSummaryResponse summary(@PathVariable("subjectId") Long subjectId) {
        return service.summary(subjectId);
    }

    @PostMapping("/subjects/{subjectId}/grades")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a grade (score 1-10, weight defaults to 1)")
    public GradeResponse add(@PathVariable("subjectId") Long subjectId,
                             @Valid @RequestBody GradeRequest request) {
        return service.add(subjectId, request);
    }

    @PutMapping("/grades/{id}")
    @Operation(summary = "Change a grade")
    public GradeResponse update(@PathVariable("id") Long id, @Valid @RequestBody GradeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/grades/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a grade")
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }
}
