package com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity;

import com.srm.mcc.credit.domain.enums.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlements", indexes = {
        @Index(name = "idx_settlements_receivable", columnList = "receivable_id"),
        @Index(name = "idx_settlements_settled_at", columnList = "settled_at"),
        @Index(name = "idx_settlements_payment_currency", columnList = "payment_currency")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementJpaEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receivable_id", nullable = false)
    private ReceivableJpaEntity receivable;

    @Column(name = "base_rate", nullable = false, precision = 19, scale = 10)
    private BigDecimal baseRate;

    @Column(name = "present_value", nullable = false, precision = 19, scale = 10)
    private BigDecimal presentValue;

    @Column(name = "exchange_rate_used", precision = 19, scale = 10)
    private BigDecimal exchangeRateUsed;

    @Column(name = "present_value_converted", nullable = false, precision = 19, scale = 10)
    private BigDecimal presentValueConverted;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_currency", nullable = false, length = 3)
    private Currency paymentCurrency;

    @Column(name = "settled_at", nullable = false)
    private LocalDateTime settledAt;
}
