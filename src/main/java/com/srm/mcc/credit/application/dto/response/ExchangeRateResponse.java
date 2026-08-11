package com.srm.mcc.credit.application.dto.response;

import com.srm.mcc.credit.domain.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExchangeRateResponse(
        UUID id,
        Currency fromCurrency,
        Currency toCurrency,
        BigDecimal rate,
        String source,
        LocalDateTime updatedAt
) {}
