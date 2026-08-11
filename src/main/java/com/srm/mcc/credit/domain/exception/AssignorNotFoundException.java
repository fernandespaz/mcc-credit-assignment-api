package com.srm.mcc.credit.domain.exception;

import java.util.UUID;

public class AssignorNotFoundException extends DomainException {
    public AssignorNotFoundException(UUID id) {
        super("Assignor not found with id: " + id);
    }
}
