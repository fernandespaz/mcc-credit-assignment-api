package com.srm.mcc.credit.application.dto.request;

import com.srm.mcc.credit.domain.enums.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateExchangeRateRequest(
        @NotNull(message = "From currency is required")
        Currency fromCurrency,

        @NotNull(message = "To currency is required")
        Currency toCurrency,

        @NotNull(message = "Rate is required")
        @DecimalMin(value = "0.0001", message = "Rate must be positive")
        BigDecimal rate
) {}
