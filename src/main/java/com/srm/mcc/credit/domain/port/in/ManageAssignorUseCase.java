package com.srm.mcc.credit.domain.port.in;

import com.srm.mcc.credit.application.dto.request.CreateAssignorRequest;
import com.srm.mcc.credit.application.dto.request.UpdateAssignorRequest;
import com.srm.mcc.credit.application.dto.response.AssignorResponse;

import java.util.List;
import java.util.UUID;

public interface ManageAssignorUseCase {
    AssignorResponse create(CreateAssignorRequest request);
    AssignorResponse findById(UUID id);
    List<AssignorResponse> findAll();
    AssignorResponse update(UUID id, UpdateAssignorRequest request);
    void deactivate(UUID id);
}
