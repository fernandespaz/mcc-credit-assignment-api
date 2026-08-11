package com.srm.mcc.credit.infrastructure.adapter.out.persistence.repository;

import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.SettlementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementJpaRepository extends JpaRepository<SettlementJpaEntity, UUID> {
    List<SettlementJpaEntity> findByReceivableId(UUID receivableId);
}
