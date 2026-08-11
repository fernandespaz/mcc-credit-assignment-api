package com.srm.mcc.credit.infrastructure.adapter.out.persistence.adapter;

import com.srm.mcc.credit.domain.entity.Assignor;
import com.srm.mcc.credit.domain.port.out.AssignorRepositoryPort;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.AssignorJpaEntity;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.repository.AssignorJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssignorRepositoryAdapter implements AssignorRepositoryPort {

    private final AssignorJpaRepository jpaRepository;

    @Override
    public Assignor save(Assignor assignor) {
        return toDomain(jpaRepository.save(toEntity(assignor)));
    }

    @Override
    public Optional<Assignor> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Assignor> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByDocument(String document) {
        return jpaRepository.existsByDocument(document);
    }

    private AssignorJpaEntity toEntity(Assignor a) {
        return AssignorJpaEntity.builder()
                .id(a.getId())
                .name(a.getName())
                .document(a.getDocument())
                .email(a.getEmail())
                .active(a.isActive())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private Assignor toDomain(AssignorJpaEntity e) {
        return Assignor.builder()
                .id(e.getId())
                .name(e.getName())
                .document(e.getDocument())
                .email(e.getEmail())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
