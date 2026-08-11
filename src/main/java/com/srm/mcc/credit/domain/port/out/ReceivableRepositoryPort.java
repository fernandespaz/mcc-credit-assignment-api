package com.srm.mcc.credit.domain.port.out;

import com.srm.mcc.credit.domain.entity.Receivable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceivableRepositoryPort {
    Receivable save(Receivable receivable);
    Optional<Receivable> findById(UUID id);

    /**
     * Acquires a pessimistic write lock on the row to prevent race conditions
     * during settlement processing.
     */
    Optional<Receivable> findByIdWithLock(UUID id);
    List<Receivable> findAll();
    List<Receivable> findByAssignorId(UUID assignorId);
}
