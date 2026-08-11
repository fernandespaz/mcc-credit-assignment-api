package com.srm.mcc.credit.domain.service.pricing;

import com.srm.mcc.credit.domain.enums.ReceivableType;
import com.srm.mcc.credit.domain.exception.DomainException;

import java.util.EnumMap;
import java.util.Map;

/**
 * Context class that selects the correct PricingStrategy based on ReceivableType.
 * Add new strategies here as new receivable types are introduced.
 */
public class PricingContext {

    private static final Map<ReceivableType, PricingStrategy> STRATEGIES =
            new EnumMap<>(ReceivableType.class);

    static {
        STRATEGIES.put(ReceivableType.DUPLICATA, new DuplicataPricingStrategy());
        STRATEGIES.put(ReceivableType.POST_DATED_CHECK, new PostDatedCheckPricingStrategy());
    }

    public static PricingStrategy strategyFor(ReceivableType type) {
        PricingStrategy strategy = STRATEGIES.get(type);
        if (strategy == null) {
            throw new DomainException("No pricing strategy registered for receivable type: " + type) {};
        }
        return strategy;
    }
}
