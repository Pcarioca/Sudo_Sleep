package com.sudosleep.backend.controller;

import com.sudosleep.backend.dto.ActivityRequirementRequest;
import com.sudosleep.backend.dto.ActivityRequirementResponse;
import com.sudosleep.backend.dto.UnscheduledRequirementResponse;
import com.sudosleep.backend.service.ActivityRequirementService;
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
@Tag(name = "Activity requirements")
public class ActivityRequirementController {

    private final ActivityRequirementService service;

    public ActivityRequirementController(ActivityRequirementService service) {
        this.service = service;
    }

    @GetMapping("/subjects/{subjectId}/requirements")
    @Operation(summary = "List the activities a subject requires (lecture, laboratory, ...)")
    public List<ActivityRequirementResponse> list(@PathVariable("subjectId") Long subjectId) {
        return service.listForSubject(subjectId);
    }

    @PostMapping("/subjects/{subjectId}/requirements")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a required activity to a subject")
    public ActivityRequirementResponse add(@PathVariable("subjectId") Long subjectId,
                                           @Valid @RequestBody ActivityRequirementRequest request) {
        return service.add(subjectId, request);
    }

    @PutMapping("/requirements/{id}")
    @Operation(summary = "Change the type of a required activity")
    public ActivityRequirementResponse update(@PathVariable("id") Long id,
                                              @Valid @RequestBody ActivityRequirementRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/requirements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a required activity and its timetable entries")
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }

    @GetMapping("/semesters/{semesterId}/requirements/unscheduled")
    @Operation(summary = "Activities of the semester that are not in the timetable yet (the warning banner)")
    public List<UnscheduledRequirementResponse> unscheduled(@PathVariable("semesterId") Long semesterId) {
        return service.listUnscheduled(semesterId);
    }
}
