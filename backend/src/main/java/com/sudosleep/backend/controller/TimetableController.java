package com.sudosleep.backend.controller;

import com.sudosleep.backend.dto.TimetableEntryRequest;
import com.sudosleep.backend.dto.TimetableEntryResponse;
import com.sudosleep.backend.service.TimetableService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Timetable")
public class TimetableController {

    private final TimetableService service;

    public TimetableController(TimetableService service) {
        this.service = service;
    }

    @GetMapping("/semesters/{semesterId}/timetable")
    @Operation(summary = "Timetable of a semester; pass week=N to get only what happens in week N")
    public List<TimetableEntryResponse> list(
            @PathVariable("semesterId") Long semesterId,
            @RequestParam(name = "week", required = false) Integer week) {
        return service.listForSemester(semesterId, week);
    }

    @PostMapping("/semesters/{semesterId}/timetable/conflicts")
    @Operation(summary = "Check a slot for collisions without saving it (use while the user picks a time)")
    public List<TimetableEntryResponse> previewConflicts(
            @PathVariable("semesterId") Long semesterId,
            @RequestParam(name = "excludeEntryId", required = false) Long excludeEntryId,
            @Valid @RequestBody TimetableEntryRequest request) {
        return service.previewConflicts(semesterId, request, excludeEntryId);
    }

    @PostMapping("/requirements/{requirementId}/timetable-entries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place a required activity in the timetable. "
            + "Answers 409 with the colliding entries unless force=true")
    public TimetableEntryResponse create(
            @PathVariable("requirementId") Long requirementId,
            @RequestParam(name = "force", defaultValue = "false") boolean force,
            @Valid @RequestBody TimetableEntryRequest request) {
        return service.create(requirementId, request, force);
    }

    @GetMapping("/timetable-entries/{id}")
    @Operation(summary = "Get a timetable entry")
    public TimetableEntryResponse get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    @PutMapping("/timetable-entries/{id}")
    @Operation(summary = "Move or edit a timetable entry. Answers 409 with the colliding entries unless force=true")
    public TimetableEntryResponse update(
            @PathVariable("id") Long id,
            @RequestParam(name = "force", defaultValue = "false") boolean force,
            @Valid @RequestBody TimetableEntryRequest request) {
        return service.update(id, request, force);
    }

    @DeleteMapping("/timetable-entries/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove an entry from the timetable (the activity becomes unscheduled again)")
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }
}
