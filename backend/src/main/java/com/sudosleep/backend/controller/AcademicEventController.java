package com.sudosleep.backend.controller;

import com.sudosleep.backend.dto.AcademicEventRequest;
import com.sudosleep.backend.dto.AcademicEventResponse;
import com.sudosleep.backend.security.AuthenticatedUser;
import com.sudosleep.backend.service.AcademicEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Academic events (Exams & Deadlines)")
public class AcademicEventController {

    private final AcademicEventService service;

    public AcademicEventController(AcademicEventService service) {
        this.service = service;
    }

    @GetMapping("/semesters/{semesterId}/events")
    @Operation(summary = "List exams, midterms, quizzes and deadlines for a semester (optional date range)")
    public List<AcademicEventResponse> list(
            @PathVariable("semesterId") Long semesterId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.listForSemester(semesterId, from, to, currentUser.id());
    }

    @PostMapping("/semesters/{semesterId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an exam, midterm, quiz, deadline or custom event in a semester")
    public AcademicEventResponse create(
            @PathVariable("semesterId") Long semesterId,
            @Valid @RequestBody AcademicEventRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.create(semesterId, currentUser.id(), request);
    }

    @GetMapping("/events/{id}")
    @Operation(summary = "Get a single academic event")
    public AcademicEventResponse get(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.get(id, currentUser.id());
    }

    @PutMapping("/events/{id}")
    @Operation(summary = "Update an academic event")
    public AcademicEventResponse update(
            @PathVariable("id") Long id,
            @Valid @RequestBody AcademicEventRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.update(id, currentUser.id(), request);
    }

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an academic event")
    public void delete(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        service.delete(id, currentUser.id());
    }
}
