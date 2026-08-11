package com.srm.mcc.credit.domain.port.in;

import com.srm.mcc.credit.application.dto.request.ExecuteSettlementRequest;
import com.srm.mcc.credit.application.dto.response.SettlementResponse;

import java.util.List;
import java.util.UUID;

public interface ExecuteSettlementUseCase {
    SettlementResponse execute(ExecuteSettlementRequest request);
    SettlementResponse findById(UUID id);
    List<SettlementResponse> findByReceivable(UUID receivableId);
}
