package com.srm.mcc.credit.application.usecase;

import com.srm.mcc.credit.application.dto.request.CreateExchangeRateRequest;
import com.srm.mcc.credit.application.dto.response.ExchangeRateResponse;
import com.srm.mcc.credit.domain.entity.ExchangeRate;
import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.domain.exception.ExchangeRateNotFoundException;
import com.srm.mcc.credit.domain.port.in.ManageExchangeRateUseCase;
import com.srm.mcc.credit.domain.port.out.ExchangeRateRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeRateApplicationService implements ManageExchangeRateUseCase {

    private final ExchangeRateRepositoryPort exchangeRateRepository;

    @Override
    @Transactional
    public ExchangeRateResponse create(CreateExchangeRateRequest request) {
        ExchangeRate rate = ExchangeRate.builder()
                .id(UUID.randomUUID())
                .fromCurrency(request.fromCurrency())
                .toCurrency(request.toCurrency())
                .rate(request.rate())
                .source("MANUAL")
                .updatedAt(LocalDateTime.now())
                .build();
        return toResponse(exchangeRateRepository.save(rate));
    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeRateResponse findById(UUID id) {
        return exchangeRateRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ExchangeRateNotFoundException(null, null) {
                    @Override
                    public String getMessage() { return "Exchange rate not found with id: " + id; }
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ExchangeRateResponse findByPair(Currency from, Currency to) {
        return exchangeRateRepository.findByPair(from, to)
                .map(this::toResponse)
                .orElseThrow(() -> new ExchangeRateNotFoundException(from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> findAll() {
        return exchangeRateRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public ExchangeRateResponse update(UUID id, CreateExchangeRateRequest request) {
        ExchangeRate existing = exchangeRateRepository.findById(id)
                .orElseThrow(() -> new ExchangeRateNotFoundException(request.fromCurrency(), request.toCurrency()));
        ExchangeRate updated = existing
                .withRate(request.rate())
                .withSource("MANUAL")
                .withUpdatedAt(LocalDateTime.now());
        return toResponse(exchangeRateRepository.save(updated));
    }

    /**
     * Mocked external rate sync. Simulates fetching USD/BRL and EUR/BRL from an external provider.
     */
    @Override
    @Transactional
    public List<ExchangeRateResponse> syncMockRates() {
        Map<String, BigDecimal> mockRates = Map.of(
                "USD_BRL", new BigDecimal("5.2150"),
                "EUR_BRL", new BigDecimal("5.6830"),
                "BRL_USD", new BigDecimal("0.1917"),
                "BRL_EUR", new BigDecimal("0.1759")
        );

        return mockRates.entrySet().stream().map(entry -> {
            String[] parts = entry.getKey().split("_");
            Currency from = Currency.valueOf(parts[0]);
            Currency to = Currency.valueOf(parts[1]);
            ExchangeRate rate = exchangeRateRepository.findByPair(from, to)
                    .map(existing -> existing.withRate(entry.getValue())
                            .withSource("MOCK_API")
                            .withUpdatedAt(LocalDateTime.now()))
                    .orElseGet(() -> ExchangeRate.builder()
                            .id(UUID.randomUUID())
                            .fromCurrency(from)
                            .toCurrency(to)
                            .rate(entry.getValue())
                            .source("MOCK_API")
                            .updatedAt(LocalDateTime.now())
                            .build());
            return toResponse(exchangeRateRepository.save(rate));
        }).toList();
    }

    private ExchangeRateResponse toResponse(ExchangeRate e) {
        return new ExchangeRateResponse(
                e.getId(), e.getFromCurrency(), e.getToCurrency(),
                e.getRate(), e.getSource(), e.getUpdatedAt()
        );
    }
}
