package com.srm.mcc.credit.infrastructure.adapter.out.persistence.repository;

import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.AssignorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AssignorJpaRepository extends JpaRepository<AssignorJpaEntity, UUID> {
    boolean existsByDocument(String document);
}
