package com.srm.mcc.credit.domain.service.pricing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Pricing strategy for Duplicata (Trade Invoice).
 * Risk spread: 1.5% per month.
 */
public class DuplicataPricingStrategy implements PricingStrategy {

    private static final BigDecimal SPREAD = new BigDecimal("0.015");
    private static final int SCALE = 10;

    @Override
    public BigDecimal calculatePresentValue(BigDecimal faceValue, BigDecimal baseRate, int termMonths) {
        // rate = 1 + baseRate + spread
        BigDecimal rate = BigDecimal.ONE.add(baseRate).add(SPREAD);
        // divisor = rate ^ term
        BigDecimal divisor = rate.pow(termMonths, MathContext.DECIMAL128);
        return faceValue.divide(divisor, SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getSpread() {
        return SPREAD;
    }
}
