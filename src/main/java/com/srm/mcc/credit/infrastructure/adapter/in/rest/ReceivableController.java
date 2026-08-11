package com.srm.mcc.credit.infrastructure.adapter.in.rest;

import com.srm.mcc.credit.application.dto.request.CreateReceivableRequest;
import com.srm.mcc.credit.application.dto.response.PresentValueResponse;
import com.srm.mcc.credit.application.dto.response.ReceivableResponse;
import com.srm.mcc.credit.domain.port.in.ManageReceivableUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receivables")
@RequiredArgsConstructor
@Tag(name = "Receivables", description = "Manage receivables (duplicatas, cheques pré-datados)")
public class ReceivableController {

    private final ManageReceivableUseCase useCase;

    @PostMapping
    @Operation(summary = "Register a new receivable")
    public ResponseEntity<ReceivableResponse> create(@Valid @RequestBody CreateReceivableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find receivable by ID")
    public ResponseEntity<ReceivableResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(useCase.findById(id));
    }

    @GetMapping
    @Operation(summary = "List all receivables")
    public ResponseEntity<List<ReceivableResponse>> findAll() {
        return ResponseEntity.ok(useCase.findAll());
    }

    @GetMapping("/by-assignor/{assignorId}")
    @Operation(summary = "List receivables by assignor")
    public ResponseEntity<List<ReceivableResponse>> findByAssignor(@PathVariable UUID assignorId) {
        return ResponseEntity.ok(useCase.findByAssignor(assignorId));
    }

    @GetMapping("/{id}/simulate")
    @Operation(summary = "Simulate present value without creating a settlement")
    public ResponseEntity<PresentValueResponse> simulate(
            @PathVariable UUID id,
            @RequestParam BigDecimal baseRate) {
        return ResponseEntity.ok(useCase.simulate(id, baseRate));
    }
}
