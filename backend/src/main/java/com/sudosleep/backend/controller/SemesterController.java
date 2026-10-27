package com.sudosleep.backend.controller;

import com.sudosleep.backend.dto.SemesterGpaResponse;
import com.sudosleep.backend.dto.SemesterRequest;
import com.sudosleep.backend.dto.SemesterResponse;
import com.sudosleep.backend.service.SemesterService;
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
@Tag(name = "Semesters")
public class SemesterController {

    private final SemesterService service;

    public SemesterController(SemesterService service) {
        this.service = service;
    }

    @GetMapping("/users/{userId}/semesters")
    @Operation(summary = "List the semesters of a user, newest first")
    public List<SemesterResponse> list(@PathVariable("userId") Long userId) {
        return service.listForUser(userId);
    }

    @PostMapping("/users/{userId}/semesters")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a semester (startDate must be the Monday of week 1)")
    public SemesterResponse create(@PathVariable("userId") Long userId,
                                   @Valid @RequestBody SemesterRequest request) {
        return service.create(userId, request);
    }

    @GetMapping("/semesters/{id}")
    @Operation(summary = "Get a semester, including the current week number")
    public SemesterResponse get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @PutMapping("/semesters/{id}")
    @Operation(summary = "Update a semester")
    public SemesterResponse update(@PathVariable("id") Long id, @Valid @RequestBody SemesterRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/semesters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a semester with its subjects, grades and timetable")
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }

    @GetMapping("/semesters/{id}/gpa")
    @Operation(summary = "Semester GPA: average of the weighted averages of the subjects that have grades")
    public SemesterGpaResponse gpa(@PathVariable("id") Long id) {
        return service.gpa(id);
    }
}
