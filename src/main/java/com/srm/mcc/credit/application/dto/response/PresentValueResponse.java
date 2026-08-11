package com.srm.mcc.credit.application.dto.response;

import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.domain.enums.ReceivableType;

import java.math.BigDecimal;

public record PresentValueResponse(
        ReceivableType receivableType,
        BigDecimal faceValue,
        Currency assetCurrency,
        BigDecimal baseRate,
        BigDecimal spread,
        int termMonths,
        BigDecimal presentValue,
        BigDecimal exchangeRateUsed,
        BigDecimal presentValueConverted,
        Currency paymentCurrency
) {}
