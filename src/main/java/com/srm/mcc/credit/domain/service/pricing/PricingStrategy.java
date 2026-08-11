package com.srm.mcc.credit.domain.service.pricing;

import java.math.BigDecimal;

/**
 * Strategy interface for present value calculation.
 * Each receivable type carries a different risk spread.
 *
 * <p>Formula: PV = FV / (1 + baseRate + spread) ^ term
 */
public interface PricingStrategy {
    BigDecimal calculatePresentValue(BigDecimal faceValue, BigDecimal baseRate, int termMonths);
    BigDecimal getSpread();
}
