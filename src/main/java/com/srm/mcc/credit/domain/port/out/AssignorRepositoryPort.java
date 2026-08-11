package com.srm.mcc.credit.domain.port.out;

import com.srm.mcc.credit.domain.entity.Assignor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignorRepositoryPort {
    Assignor save(Assignor assignor);
    Optional<Assignor> findById(UUID id);
    List<Assignor> findAll();
    boolean existsByDocument(String document);
}
