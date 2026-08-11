package com.srm.mcc.credit.domain.entity;

import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.domain.enums.ReceivableType;
import com.srm.mcc.credit.domain.enums.SettlementStatus;
import com.srm.mcc.credit.domain.exception.ReceivableAlreadySettledException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@With
@AllArgsConstructor
public class Receivable {

    private final UUID id;
    private final Assignor assignor;
    private final ReceivableType type;
    private final BigDecimal faceValue;
    private final Currency assetCurrency;    // currency in which the face value is denominated
    private final Currency paymentCurrency;  // currency in which settlement will be paid
    private final LocalDate maturityDate;
    private final int termMonths;            // discount term in months
    private final SettlementStatus status;
    private final LocalDateTime createdAt;

    public boolean isCrossCurrency() {
        return !assetCurrency.equals(paymentCurrency);
    }

    public boolean isSettled() {
        return SettlementStatus.SETTLED.equals(status);
    }

    public boolean isCancelled() {
        return SettlementStatus.CANCELLED.equals(status);
    }

    /**
     * Transitions the receivable to SETTLED state.
     * Throws if already settled or cancelled.
     */
    public Receivable markSettled() {
        if (isSettled() || isCancelled()) {
            throw new ReceivableAlreadySettledException(id, status);
        }
        return this.withStatus(SettlementStatus.SETTLED);
    }

    public Receivable cancel() {
        if (isSettled()) {
            throw new ReceivableAlreadySettledException(id, status);
        }
        return this.withStatus(SettlementStatus.CANCELLED);
    }
}
