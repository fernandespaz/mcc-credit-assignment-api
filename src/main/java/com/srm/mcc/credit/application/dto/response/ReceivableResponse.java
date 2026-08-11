package com.srm.mcc.credit.application.dto.response;

import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.domain.enums.ReceivableType;
import com.srm.mcc.credit.domain.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReceivableResponse(
        UUID id,
        UUID assignorId,
        String assignorName,
        ReceivableType type,
        BigDecimal faceValue,
        Currency assetCurrency,
        Currency paymentCurrency,
        boolean crossCurrency,
        LocalDate maturityDate,
        int termMonths,
        SettlementStatus status,
        LocalDateTime createdAt
) {}
