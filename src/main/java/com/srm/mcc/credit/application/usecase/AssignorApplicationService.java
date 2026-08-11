package com.srm.mcc.credit.application.usecase;

import com.srm.mcc.credit.application.dto.request.CreateAssignorRequest;
import com.srm.mcc.credit.application.dto.request.UpdateAssignorRequest;
import com.srm.mcc.credit.application.dto.response.AssignorResponse;
import com.srm.mcc.credit.domain.entity.Assignor;
import com.srm.mcc.credit.domain.exception.AssignorNotFoundException;
import com.srm.mcc.credit.domain.port.in.ManageAssignorUseCase;
import com.srm.mcc.credit.domain.port.out.AssignorRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignorApplicationService implements ManageAssignorUseCase {

    private final AssignorRepositoryPort assignorRepository;

    @Override
    @Transactional
    public AssignorResponse create(CreateAssignorRequest request) {
        Assignor assignor = Assignor.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .document(request.document())
                .email(request.email())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return toResponse(assignorRepository.save(assignor));
    }

    @Override
    @Transactional(readOnly = true)
    public AssignorResponse findById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignorResponse> findAll() {
        return assignorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public AssignorResponse update(UUID id, UpdateAssignorRequest request) {
        Assignor existing = findOrThrow(id);
        Assignor updated = existing
                .withName(request.name() != null ? request.name() : existing.getName())
                .withEmail(request.email() != null ? request.email() : existing.getEmail())
                .withUpdatedAt(LocalDateTime.now());
        return toResponse(assignorRepository.save(updated));
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        Assignor assignor = findOrThrow(id);
        assignorRepository.save(assignor.deactivate());
    }

    private Assignor findOrThrow(UUID id) {
        return assignorRepository.findById(id)
                .orElseThrow(() -> new AssignorNotFoundException(id));
    }

    private AssignorResponse toResponse(Assignor a) {
        return new AssignorResponse(
                a.getId(), a.getName(), a.getDocument(),
                a.getEmail(), a.isActive(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
