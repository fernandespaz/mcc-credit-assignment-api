package com.srm.mcc.credit.domain.entity;

import com.srm.mcc.credit.domain.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class ExchangeRate {

    private final UUID id;
    private final Currency fromCurrency;
    private final Currency toCurrency;
    private final BigDecimal rate;
    private final String source; // MANUAL or MOCK_API
    private final LocalDateTime updatedAt;
}
