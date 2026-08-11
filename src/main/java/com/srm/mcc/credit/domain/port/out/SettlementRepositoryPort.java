package com.srm.mcc.credit.domain.port.out;

import com.srm.mcc.credit.domain.entity.Settlement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepositoryPort {
    Settlement save(Settlement settlement);
    Optional<Settlement> findById(UUID id);
    List<Settlement> findByReceivableId(UUID receivableId);
}
