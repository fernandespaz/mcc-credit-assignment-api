package com.srm.mcc.credit.domain.exception;

import com.srm.mcc.credit.domain.enums.Currency;

public class ExchangeRateNotFoundException extends DomainException {
    public ExchangeRateNotFoundException(Currency from, Currency to) {
        super("Exchange rate not found for pair: " + from + " -> " + to);
    }
}
