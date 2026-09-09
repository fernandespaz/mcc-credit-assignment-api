package com.srm.mcc.credit.infrastructure.adapter.out.persistence.repository;

import com.srm.mcc.credit.application.dto.response.SettlementStatementResponse;
import com.srm.mcc.credit.domain.enums.Currency;
import com.srm.mcc.credit.domain.enums.ReceivableType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Settlement statement query using native SQL — bypasses the domain/application layer entirely.
 * This is the 2-layer report path: Controller → SettlementStatementQueryRepository.
 *
 * <p>Native SQL is preferred here for performance: large data sets, complex joins,
 * and filtering that would be verbose and slow via ORM abstraction.
 */
@Repository
@Slf4j
public class SettlementStatementQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<SettlementStatementResponse> query(
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID assignorId,
            Currency paymentCurrency,
            int page,
            int size
    ) {
        String sql = """
                SELECT
                    s.id              AS settlement_id,
                    s.settled_at,
                    a.id              AS assignor_id,
                    a.name            AS assignor_name,
                    a.document        AS assignor_document,
                    r.id              AS receivable_id,
                    r.type            AS receivable_type,
                    r.face_value,
                    r.asset_currency,
                    s.base_rate,
                    s.present_value,
                    s.exchange_rate_used,
                    s.present_value_converted,
                    s.payment_currency
                FROM settlements s
                JOIN receivables r  ON s.receivable_id = r.id
                JOIN assignors  a  ON r.assignor_id    = a.id
                WHERE (CAST(:startDate AS TIMESTAMP)       IS NULL OR s.settled_at       >= CAST(:startDate AS TIMESTAMP))
                  AND (CAST(:endDate AS TIMESTAMP)         IS NULL OR s.settled_at       <= CAST(:endDate AS TIMESTAMP))
                  AND (CAST(:assignorId AS VARCHAR)        IS NULL OR CAST(a.id AS VARCHAR) = CAST(:assignorId AS VARCHAR))
                  AND (CAST(:paymentCurrency AS VARCHAR)   IS NULL OR s.payment_currency  = CAST(:paymentCurrency AS VARCHAR))
                ORDER BY s.settled_at DESC
                """;

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setParameter("assignorId", assignorId != null ? assignorId.toString() : null)
                .setParameter("paymentCurrency", paymentCurrency != null ? paymentCurrency.name() : null)
                .setFirstResult(page * size)
                .setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<SettlementStatementResponse> result = new ArrayList<>(rows.size());

        for (Object[] row : rows) {
            result.add(new SettlementStatementResponse(
                    UUID.fromString(row[0].toString()),
                    toLocalDateTime(row[1]),
                    UUID.fromString(row[2].toString()),
                    (String) row[3],
                    (String) row[4],
                    UUID.fromString(row[5].toString()),
                    ReceivableType.valueOf((String) row[6]),
                    (BigDecimal) row[7],
                    Currency.valueOf((String) row[8]),
                    (BigDecimal) row[9],
                    (BigDecimal) row[10],
                    row[11] != null ? (BigDecimal) row[11] : null,
                    (BigDecimal) row[12],
                    Currency.valueOf((String) row[13])
            ));
        }

        return result;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof Timestamp ts) return ts.toLocalDateTime();
        throw new IllegalArgumentException("Cannot convert to LocalDateTime: " + value);
    }
}
