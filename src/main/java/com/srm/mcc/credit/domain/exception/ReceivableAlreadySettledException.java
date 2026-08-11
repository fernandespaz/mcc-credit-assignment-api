package com.srm.mcc.credit.domain.exception;

import com.srm.mcc.credit.domain.enums.SettlementStatus;

import java.util.UUID;

public class ReceivableAlreadySettledException extends DomainException {
    public ReceivableAlreadySettledException(UUID receivableId, SettlementStatus currentStatus) {
        super("Receivable " + receivableId + " cannot be settled — current status: " + currentStatus);
    }
}
