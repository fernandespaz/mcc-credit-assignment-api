package com.srm.mcc.credit.application.dto.response;

import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.domain.enums.ReceivableType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SettlementResponse(
        UUID id,
        UUID receivableId,
        ReceivableType receivableType,
        UUID assignorId,
        String assignorName,
        BigDecimal faceValue,
        BigDecimal baseRate,
        BigDecimal spread,
        BigDecimal presentValue,
        Currency assetCurrency,
        BigDecimal exchangeRateUsed,
        BigDecimal presentValueConverted,
        Currency paymentCurrency,
        LocalDateTime settledAt
) {}
