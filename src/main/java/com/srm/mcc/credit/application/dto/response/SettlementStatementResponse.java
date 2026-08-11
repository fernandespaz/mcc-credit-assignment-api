package com.srm.mcc.credit.application.dto.response;

import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.domain.enums.ReceivableType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SettlementStatementResponse(
        UUID settlementId,
        LocalDateTime settledAt,
        UUID assignorId,
        String assignorName,
        String assignorDocument,
        UUID receivableId,
        ReceivableType receivableType,
        BigDecimal faceValue,
        Currency assetCurrency,
        BigDecimal baseRate,
        BigDecimal presentValue,
        BigDecimal exchangeRateUsed,
        BigDecimal presentValueConverted,
        Currency paymentCurrency
) {}
