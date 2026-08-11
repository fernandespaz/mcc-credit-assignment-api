package com.srm.mcc.credit.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ExecuteSettlementRequest(
        @NotNull(message = "Receivable ID is required")
        UUID receivableId,

        @NotNull(message = "Base rate is required")
        @DecimalMin(value = "0.0", message = "Base rate cannot be negative")
        BigDecimal baseRate
) {}
