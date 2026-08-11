package com.srm.mcc.credit.domain.entity;

import com.srm.mcc.credit.domain.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class Settlement {

    private final UUID id;
    private final Receivable receivable;
    private final BigDecimal baseRate;           // base rate used (e.g. CDI/Selic, monthly)
    private final BigDecimal presentValue;       // PV in asset currency
    private final BigDecimal exchangeRateUsed;   // conversion rate applied (null if same currency)
    private final BigDecimal presentValueConverted; // PV in payment currency (equals presentValue if same-currency)
    private final Currency paymentCurrency;
    private final LocalDateTime settledAt;
}
