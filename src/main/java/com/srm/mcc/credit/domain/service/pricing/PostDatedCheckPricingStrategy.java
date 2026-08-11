package com.srm.mcc.credit.domain.service.pricing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Pricing strategy for Post-dated Check (Cheque Pré-datado).
 * Risk spread: 2.5% per month (higher risk than duplicata).
 */
public class PostDatedCheckPricingStrategy implements PricingStrategy {

    private static final BigDecimal SPREAD = new BigDecimal("0.025");
    private static final int SCALE = 10;

    @Override
    public BigDecimal calculatePresentValue(BigDecimal faceValue, BigDecimal baseRate, int termMonths) {
        BigDecimal rate = BigDecimal.ONE.add(baseRate).add(SPREAD);
        BigDecimal divisor = rate.pow(termMonths, MathContext.DECIMAL128);
        return faceValue.divide(divisor, SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal getSpread() {
        return SPREAD;
    }
}
