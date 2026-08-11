package com.srm.mcc.credit.domain.exception;

import java.util.UUID;

public class SettlementNotFoundException extends DomainException {
    public SettlementNotFoundException(UUID id) {
        super("Settlement not found with id: " + id);
    }
}
