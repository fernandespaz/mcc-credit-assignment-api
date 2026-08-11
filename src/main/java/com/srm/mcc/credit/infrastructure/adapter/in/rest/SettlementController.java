package com.srm.mcc.credit.infrastructure.adapter.in.rest;

import com.srm.mcc.credit.application.dto.request.ExecuteSettlementRequest;
import com.srm.mcc.credit.application.dto.response.SettlementResponse;
import com.srm.mcc.credit.domain.port.in.ExecuteSettlementUseCase;
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
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
@Tag(name = "Settlements", description = "Execute and query settlements (liquidações)")
public class SettlementController {

    private final ExecuteSettlementUseCase useCase;

    @PostMapping
    @Operation(summary = "Execute a settlement for a receivable",
            description = "Applies pricing strategy (spread by type), calculates PV, applies FX if cross-currency. ACID-safe with pessimistic lock.")
    public ResponseEntity<SettlementResponse> execute(@Valid @RequestBody ExecuteSettlementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.execute(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find settlement by ID")
    public ResponseEntity<SettlementResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(useCase.findById(id));
    }

    @GetMapping("/by-receivable/{receivableId}")
    @Operation(summary = "Find all settlements for a receivable")
    public ResponseEntity<List<SettlementResponse>> findByReceivable(@PathVariable UUID receivableId) {
        return ResponseEntity.ok(useCase.findByReceivable(receivableId));
    }
}
