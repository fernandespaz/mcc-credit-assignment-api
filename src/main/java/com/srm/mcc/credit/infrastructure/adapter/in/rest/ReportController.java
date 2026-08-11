package com.srm.mcc.credit.infrastructure.adapter.in.rest;

import com.srm.mcc.credit.application.dto.response.SettlementStatementResponse;
import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.repository.SettlementStatementQueryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Report controller — 2-layer architecture (Controller → Repository).
 * Bypasses the domain and application layers intentionally for query-only reporting.
 * Uses native SQL for performance with large data volumes.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Analytical settlement reports — direct SQL, 2-layer path")
public class ReportController {

    private final SettlementStatementQueryRepository queryRepository;

    @GetMapping("/settlement-statement")
    @Operation(
            summary = "Settlement Statement",
            description = "Returns paginated settlements filtered by period, assignor, and/or currency. Uses optimized native SQL."
    )
    public ResponseEntity<List<SettlementStatementResponse>> settlementStatement(
            @Parameter(description = "Start date (ISO 8601, e.g. 2024-01-01T00:00:00)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,

            @Parameter(description = "End date (ISO 8601, e.g. 2024-12-31T23:59:59)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,

            @Parameter(description = "Filter by assignor UUID")
            @RequestParam(required = false)
            UUID assignorId,

            @Parameter(description = "Filter by payment currency (BRL, USD, EUR)")
            @RequestParam(required = false)
            Currency paymentCurrency,

            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0")
            int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20")
            int size
    ) {
        List<SettlementStatementResponse> result =
                queryRepository.query(startDate, endDate, assignorId, paymentCurrency, page, size);
        return ResponseEntity.ok(result);
    }
}
