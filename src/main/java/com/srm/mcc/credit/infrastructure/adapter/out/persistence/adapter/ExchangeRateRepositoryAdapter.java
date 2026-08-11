package com.srm.mcc.credit.infrastructure.adapter.out.persistence.adapter;

import com.srm.mcc.credit.domain.entity.ExchangeRate;
import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.domain.port.out.ExchangeRateRepositoryPort;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.ExchangeRateJpaEntity;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.repository.ExchangeRateJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ExchangeRateRepositoryAdapter implements ExchangeRateRepositoryPort {

    private final ExchangeRateJpaRepository jpaRepository;

    @Override
    public ExchangeRate save(ExchangeRate rate) {
        return toDomain(jpaRepository.save(toEntity(rate)));
    }

    @Override
    public Optional<ExchangeRate> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<ExchangeRate> findByPair(Currency from, Currency to) {
        return jpaRepository.findByFromCurrencyAndToCurrency(from, to).map(this::toDomain);
    }

    @Override
    public List<ExchangeRate> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    private ExchangeRateJpaEntity toEntity(ExchangeRate e) {
        return ExchangeRateJpaEntity.builder()
                .id(e.getId())
                .fromCurrency(e.getFromCurrency())
                .toCurrency(e.getToCurrency())
                .rate(e.getRate())
                .source(e.getSource())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private ExchangeRate toDomain(ExchangeRateJpaEntity e) {
        return ExchangeRate.builder()
                .id(e.getId())
                .fromCurrency(e.getFromCurrency())
                .toCurrency(e.getToCurrency())
                .rate(e.getRate())
                .source(e.getSource())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
