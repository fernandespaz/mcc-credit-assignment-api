package com.srm.mcc.credit.infrastructure.adapter.out.persistence.repository;

import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.ExchangeRateJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateJpaRepository extends JpaRepository<ExchangeRateJpaEntity, UUID> {
    Optional<ExchangeRateJpaEntity> findByFromCurrencyAndToCurrency(Currency from, Currency to);
}
