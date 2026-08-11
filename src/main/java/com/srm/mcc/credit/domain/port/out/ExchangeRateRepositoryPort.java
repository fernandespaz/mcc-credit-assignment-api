package com.srm.mcc.credit.domain.port.out;

import com.srm.mcc.credit.domain.entity.ExchangeRate;
import com.srm.mcc.credit.domain.enums.Currency;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRateRepositoryPort {
    ExchangeRate save(ExchangeRate exchangeRate);
    Optional<ExchangeRate> findById(UUID id);
    Optional<ExchangeRate> findByPair(Currency from, Currency to);
    List<ExchangeRate> findAll();
}
