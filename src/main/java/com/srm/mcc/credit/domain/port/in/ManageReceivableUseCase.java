package com.srm.mcc.credit.domain.port.in;

import com.srm.mcc.credit.application.dto.request.CreateReceivableRequest;
import com.srm.mcc.credit.application.dto.response.PresentValueResponse;
import com.srm.mcc.credit.application.dto.response.ReceivableResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageReceivableUseCase {
    ReceivableResponse create(CreateReceivableRequest request);
    ReceivableResponse findById(UUID id);
    List<ReceivableResponse> findAll();
    List<ReceivableResponse> findByAssignor(UUID assignorId);

    /** Simulates the present value without persisting a settlement. */
    PresentValueResponse simulate(UUID receivableId, BigDecimal baseRate);
}
