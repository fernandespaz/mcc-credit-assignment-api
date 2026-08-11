package com.srm.mcc.credit.domain.exception;

import java.util.UUID;

public class ReceivableNotFoundException extends DomainException {
    public ReceivableNotFoundException(UUID id) {
        super("Receivable not found with id: " + id);
    }
}
