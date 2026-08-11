package com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity;

import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.domain.enums.ReceivableType;
import com.srm.mcc.credit.domain.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "receivables", indexes = {
        @Index(name = "idx_receivables_assignor", columnList = "assignor_id"),
        @Index(name = "idx_receivables_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivableJpaEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignor_id", nullable = false)
    private AssignorJpaEntity assignor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReceivableType type;

    @Column(name = "face_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal faceValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_currency", nullable = false, length = 3)
    private Currency assetCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_currency", nullable = false, length = 3)
    private Currency paymentCurrency;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
