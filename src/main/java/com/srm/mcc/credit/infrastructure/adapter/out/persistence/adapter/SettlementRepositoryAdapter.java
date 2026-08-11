package com.srm.mcc.credit.infrastructure.adapter.out.persistence.adapter;

import com.srm.mcc.credit.domain.entity.Assignor;
import com.srm.mcc.credit.domain.entity.Receivable;
import com.srm.mcc.credit.domain.entity.Settlement;
import com.srm.mcc.credit.domain.port.out.SettlementRepositoryPort;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.AssignorJpaEntity;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.ReceivableJpaEntity;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity.SettlementJpaEntity;
import com.srm.mcc.credit.infrastructure.adapter.out.persistence.repository.SettlementJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SettlementRepositoryAdapter implements SettlementRepositoryPort {

    private final SettlementJpaRepository jpaRepository;

    @Override
    public Settlement save(Settlement settlement) {
        return toDomain(jpaRepository.save(toEntity(settlement)));
    }

    @Override
    public Optional<Settlement> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Settlement> findByReceivableId(UUID receivableId) {
        return jpaRepository.findByReceivableId(receivableId).stream().map(this::toDomain).toList();
    }

    private SettlementJpaEntity toEntity(Settlement s) {
        Receivable r = s.getReceivable();
        AssignorJpaEntity assignorRef = AssignorJpaEntity.builder()
                .id(r.getAssignor().getId()).name(r.getAssignor().getName())
                .document(r.getAssignor().getDocument()).email(r.getAssignor().getEmail())
                .active(r.getAssignor().isActive()).createdAt(r.getAssignor().getCreatedAt())
                .updatedAt(r.getAssignor().getUpdatedAt()).build();

        ReceivableJpaEntity receivableRef = ReceivableJpaEntity.builder()
                .id(r.getId()).assignor(assignorRef).type(r.getType())
                .faceValue(r.getFaceValue()).assetCurrency(r.getAssetCurrency())
                .paymentCurrency(r.getPaymentCurrency()).maturityDate(r.getMaturityDate())
                .termMonths(r.getTermMonths()).status(r.getStatus()).createdAt(r.getCreatedAt())
                .build();

        return SettlementJpaEntity.builder()
                .id(s.getId()).receivable(receivableRef)
                .baseRate(s.getBaseRate()).presentValue(s.getPresentValue())
                .exchangeRateUsed(s.getExchangeRateUsed())
                .presentValueConverted(s.getPresentValueConverted())
                .paymentCurrency(s.getPaymentCurrency()).settledAt(s.getSettledAt())
                .build();
    }

    private Settlement toDomain(SettlementJpaEntity e) {
        ReceivableJpaEntity re = e.getReceivable();
        Assignor assignor = Assignor.builder()
                .id(re.getAssignor().getId()).name(re.getAssignor().getName())
                .document(re.getAssignor().getDocument()).email(re.getAssignor().getEmail())
                .active(re.getAssignor().isActive()).createdAt(re.getAssignor().getCreatedAt())
                .updatedAt(re.getAssignor().getUpdatedAt()).build();

        Receivable receivable = Receivable.builder()
                .id(re.getId()).assignor(assignor).type(re.getType())
                .faceValue(re.getFaceValue()).assetCurrency(re.getAssetCurrency())
                .paymentCurrency(re.getPaymentCurrency()).maturityDate(re.getMaturityDate())
                .termMonths(re.getTermMonths()).status(re.getStatus()).createdAt(re.getCreatedAt())
                .build();

        return Settlement.builder()
                .id(e.getId()).receivable(receivable)
                .baseRate(e.getBaseRate()).presentValue(e.getPresentValue())
                .exchangeRateUsed(e.getExchangeRateUsed())
                .presentValueConverted(e.getPresentValueConverted())
                .paymentCurrency(e.getPaymentCurrency()).settledAt(e.getSettledAt())
                .build();
    }
}
