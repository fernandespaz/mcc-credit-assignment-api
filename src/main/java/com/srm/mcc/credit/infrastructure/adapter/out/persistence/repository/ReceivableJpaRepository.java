package com.srm.mcc.credit.infrastructure.adapter.out.persistence.repository;

import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.ReceivableJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReceivableJpaRepository extends JpaRepository<ReceivableJpaEntity, UUID> {

    List<ReceivableJpaEntity> findByAssignorId(UUID assignorId);

    /**
     * Acquires a pessimistic write lock (SELECT FOR UPDATE) to prevent concurrent settlements.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReceivableJpaEntity r WHERE r.id = :id")
    Optional<ReceivableJpaEntity> findByIdWithLock(@Param("id") UUID id);
}
