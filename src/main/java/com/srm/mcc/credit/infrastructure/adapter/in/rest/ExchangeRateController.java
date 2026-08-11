package com.srm.mcc.credit.infrastructure.adapter.in.rest;

import com.srm.mcc.credit.application.dto.request.CreateExchangeRateRequest;
import com.srm.mcc.credit.application.dto.response.ExchangeRateResponse;
import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.domain.port.in.ManageExchangeRateUseCase;
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
@RequestMapping("/api/v1/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Exchange Rates", description = "Manage and sync currency exchange rates")
public class ExchangeRateController {

    private final ManageExchangeRateUseCase useCase;

    @PostMapping
    @Operation(summary = "Register a manual exchange rate")
    public ResponseEntity<ExchangeRateResponse> create(@Valid @RequestBody CreateExchangeRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find exchange rate by ID")
    public ResponseEntity<ExchangeRateResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(useCase.findById(id));
    }

    @GetMapping("/pair")
    @Operation(summary = "Find exchange rate by currency pair (e.g. ?from=USD&to=BRL)")
    public ResponseEntity<ExchangeRateResponse> findByPair(
            @RequestParam Currency from,
            @RequestParam Currency to) {
        return ResponseEntity.ok(useCase.findByPair(from, to));
    }

    @GetMapping
    @Operation(summary = "List all exchange rates")
    public ResponseEntity<List<ExchangeRateResponse>> findAll() {
        return ResponseEntity.ok(useCase.findAll());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an exchange rate manually")
    public ResponseEntity<ExchangeRateResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateExchangeRateRequest request) {
        return ResponseEntity.ok(useCase.update(id, request));
    }

    @PostMapping("/sync-mock")
    @Operation(summary = "Sync rates from mocked external provider (USD/BRL, EUR/BRL and inverses)")
    public ResponseEntity<List<ExchangeRateResponse>> syncMock() {
        return ResponseEntity.ok(useCase.syncMockRates());
    }
}
