package com.srm.mcc.credit.domain.port.in;

import com.srm.mcc.credit.application.dto.request.CreateExchangeRateRequest;
import com.srm.mcc.credit.application.dto.response.ExchangeRateResponse;
import com.srm.mcc.credit.domain.enums.Currency;

import java.util.List;
import java.util.UUID;

public interface ManageExchangeRateUseCase {
    ExchangeRateResponse create(CreateExchangeRateRequest request);
    ExchangeRateResponse findById(UUID id);
    ExchangeRateResponse findByPair(Currency from, Currency to);
    List<ExchangeRateResponse> findAll();
    ExchangeRateResponse update(UUID id, CreateExchangeRateRequest request);

    /** Fetches and stores rates from a mocked external provider. */
    List<ExchangeRateResponse> syncMockRates();
}
