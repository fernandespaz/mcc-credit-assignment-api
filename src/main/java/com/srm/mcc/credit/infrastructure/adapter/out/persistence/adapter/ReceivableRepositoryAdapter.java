package com.srm.mcc.credit.infrastructure.adapter.out.persistence.adapter;

import com.srm.mcc.credit.domain.entity.Assignor;
import com.srm.mcc.credit.domain.entity.Receivable;
import com.srm.mcc.credit.domain.port.out.ReceivableRepositoryPort;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.AssignorJpaEntity;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.ReceivableJpaEntity;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.repository.ReceivableJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReceivableRepositoryAdapter implements ReceivableRepositoryPort {

    private final ReceivableJpaRepository jpaRepository;

    @Override
    public Receivable save(Receivable receivable) {
        return toDomain(jpaRepository.save(toEntity(receivable)));
    }

    @Override
    public Optional<Receivable> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Receivable> findByIdWithLock(UUID id) {
        return jpaRepository.findByIdWithLock(id).map(this::toDomain);
    }

    @Override
    public List<Receivable> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Receivable> findByAssignorId(UUID assignorId) {
        return jpaRepository.findByAssignorId(assignorId).stream().map(this::toDomain).toList();
    }

    private ReceivableJpaEntity toEntity(Receivable r) {
        AssignorJpaEntity assignorRef = AssignorJpaEntity.builder()
                .id(r.getAssignor().getId())
                .name(r.getAssignor().getName())
                .document(r.getAssignor().getDocument())
                .email(r.getAssignor().getEmail())
                .active(r.getAssignor().isActive())
                .createdAt(r.getAssignor().getCreatedAt())
                .updatedAt(r.getAssignor().getUpdatedAt())
                .build();

        return ReceivableJpaEntity.builder()
                .id(r.getId())
                .assignor(assignorRef)
                .type(r.getType())
                .faceValue(r.getFaceValue())
                .assetCurrency(r.getAssetCurrency())
                .paymentCurrency(r.getPaymentCurrency())
                .maturityDate(r.getMaturityDate())
                .termMonths(r.getTermMonths())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private Receivable toDomain(ReceivableJpaEntity e) {
        Assignor assignor = Assignor.builder()
                .id(e.getAssignor().getId())
                .name(e.getAssignor().getName())
                .document(e.getAssignor().getDocument())
                .email(e.getAssignor().getEmail())
                .active(e.getAssignor().isActive())
                .createdAt(e.getAssignor().getCreatedAt())
                .updatedAt(e.getAssignor().getUpdatedAt())
                .build();

        return Receivable.builder()
                .id(e.getId())
                .assignor(assignor)
                .type(e.getType())
                .faceValue(e.getFaceValue())
                .assetCurrency(e.getAssetCurrency())
                .paymentCurrency(e.getPaymentCurrency())
                .maturityDate(e.getMaturityDate())
                .termMonths(e.getTermMonths())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
