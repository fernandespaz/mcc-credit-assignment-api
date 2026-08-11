package com.srm.mcc.credit.application.dto.request;

import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.domain.enums.ReceivableType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateReceivableRequest(
        @NotNull(message = "Assignor ID is required")
        UUID assignorId,

        @NotNull(message = "Receivable type is required")
        ReceivableType type,

        @NotNull(message = "Face value is required")
        @DecimalMin(value = "0.01", message = "Face value must be positive")
        BigDecimal faceValue,

        @NotNull(message = "Asset currency is required")
        Currency assetCurrency,

        @NotNull(message = "Payment currency is required")
        Currency paymentCurrency,

        @NotNull(message = "Maturity date is required")
        LocalDate maturityDate,

        @Min(value = 1, message = "Term must be at least 1 month")
        int termMonths
) {}
