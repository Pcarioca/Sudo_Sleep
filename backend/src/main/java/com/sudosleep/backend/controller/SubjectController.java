package com.sudosleep.backend.controller;

import com.sudosleep.backend.dto.SubjectRequest;
import com.sudosleep.backend.dto.SubjectResponse;
import com.sudosleep.backend.dto.SubjectUpdateRequest;
import com.sudosleep.backend.security.AuthenticatedUser;
import com.sudosleep.backend.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@Tag(name = "Subjects")
public class SubjectController {

    private final SubjectService service;

    public SubjectController(SubjectService service) {
        this.service = service;
    }

    @GetMapping("/semesters/{semesterId}/subjects")
    @Operation(summary = "List the subjects of a semester with their activities and grade averages")
    public List<SubjectResponse> list(@PathVariable("semesterId") Long semesterId,
                                      @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.listForSemester(semesterId, currentUser.id());
    }

    @PostMapping("/semesters/{semesterId}/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a subject, optionally together with its required activities")
    public SubjectResponse create(@PathVariable("semesterId") Long semesterId,
                                  @Valid @RequestBody SubjectRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.create(semesterId, currentUser.id(), request);
    }

    @GetMapping("/subjects/{id}")
    @Operation(summary = "Get a subject")
    public SubjectResponse get(@PathVariable("id") Long id,
                               @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.get(id, currentUser.id());
    }

    @PutMapping("/subjects/{id}")
    @Operation(summary = "Rename a subject or change its color")
    public SubjectResponse update(@PathVariable("id") Long id,
                                  @Valid @RequestBody SubjectUpdateRequest request,
                                  @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.update(id, currentUser.id(), request);
    }

    @DeleteMapping("/subjects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a subject with its activities, grades and timetable entries")
    public void delete(@PathVariable("id") Long id,
                       @AuthenticationPrincipal AuthenticatedUser currentUser) {
        service.delete(id, currentUser.id());
    }
}
