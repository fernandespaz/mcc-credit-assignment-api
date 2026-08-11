package com.srm.mcc.credit.infrastructure.adapter.in.rest;

import com.srm.mcc.credit.application.dto.request.CreateAssignorRequest;
import com.srm.mcc.credit.application.dto.request.UpdateAssignorRequest;
import com.srm.mcc.credit.application.dto.response.AssignorResponse;
import com.srm.mcc.credit.domain.port.in.ManageAssignorUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assignors")
@RequiredArgsConstructor
@Tag(name = "Assignors", description = "Manage credit assignors (cedentes)")
public class AssignorController {

    private final ManageAssignorUseCase useCase;

    @PostMapping
    @Operation(summary = "Create a new assignor")
    public ResponseEntity<AssignorResponse> create(@Valid @RequestBody CreateAssignorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find assignor by ID")
    public ResponseEntity<AssignorResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(useCase.findById(id));
    }

    @GetMapping
    @Operation(summary = "List all assignors")
    public ResponseEntity<List<AssignorResponse>> findAll() {
        return ResponseEntity.ok(useCase.findAll());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update assignor name or email")
    public ResponseEntity<AssignorResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssignorRequest request) {
        return ResponseEntity.ok(useCase.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate assignor (soft delete)")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        useCase.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
