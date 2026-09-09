package com.srm.mcc.credit.infrastructure.adapter.out.persistence.entity;

import com.srm.mcc.credit.domain.enums.Currency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exchange_rates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_exchange_rates_pair", columnNames = {"from_currency", "to_currency"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateJpaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_currency", nullable = false, length = 3)
    private Currency fromCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_currency", nullable = false, length = 3)
    private Currency toCurrency;

    @Column(nullable = false, precision = 19, scale = 10)
    private BigDecimal rate;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
