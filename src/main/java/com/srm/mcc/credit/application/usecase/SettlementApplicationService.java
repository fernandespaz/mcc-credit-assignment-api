package com.srm.mcc.credit.application.usecase;

import com.srm.mcc.credit.application.dto.request.ExecuteSettlementRequest;
import com.srm.mcc.credit.application.dto.response.SettlementResponse;
import com.srm.mcc.credit.domain.entity.ExchangeRate;
import com.srm.mcc.credit.domain.entity.Receivable;
import com.srm.mcc.credit.domain.entity.Settlement;
import com.srm.mcc.credit.domain.exception.ExchangeRateNotFoundException;
import com.srm.mcc.credit.domain.exception.ReceivableNotFoundException;
import com.srm.mcc.credit.domain.exception.SettlementNotFoundException;
import com.srm.mcc.credit.domain.port.in.ExecuteSettlementUseCase;
import com.srm.mcc.credit.domain.port.out.ExchangeRateRepositoryPort;
import com.srm.mcc.credit.domain.port.out.ReceivableRepositoryPort;
import com.srm.mcc.credit.domain.port.out.SettlementRepositoryPort;
import com.srm.mcc.credit.domain.service.pricing.PricingContext;
import com.srm.mcc.credit.domain.service.pricing.PricingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementApplicationService implements ExecuteSettlementUseCase {

    private final SettlementRepositoryPort settlementRepository;
    private final ReceivableRepositoryPort receivableRepository;
    private final ExchangeRateRepositoryPort exchangeRateRepository;

    /**
     * Executes settlement with SERIALIZABLE isolation and pessimistic lock on the receivable row
     * to prevent race conditions (e.g., double-settlement of the same receivable).
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SettlementResponse execute(ExecuteSettlementRequest request) {
        // Pessimistic write lock — blocks concurrent settlement attempts on same receivable
        Receivable receivable = receivableRepository.findByIdWithLock(request.receivableId())
                .orElseThrow(() -> new ReceivableNotFoundException(request.receivableId()));

        // Domain validation: throws if already SETTLED or CANCELLED
        Receivable settled = receivable.markSettled();
        receivableRepository.save(settled);

        // Pricing engine — Strategy pattern selects correct spread by type
        PricingStrategy strategy = PricingContext.strategyFor(receivable.getType());
        BigDecimal pv = strategy.calculatePresentValue(
                receivable.getFaceValue(), request.baseRate(), receivable.getTermMonths());

        BigDecimal exchangeRateUsed = null;
        BigDecimal pvConverted = pv;

        if (receivable.isCrossCurrency()) {
            ExchangeRate rate = exchangeRateRepository
                    .findByPair(receivable.getAssetCurrency(), receivable.getPaymentCurrency())
                    .orElseThrow(() -> new ExchangeRateNotFoundException(
                            receivable.getAssetCurrency(), receivable.getPaymentCurrency()));
            exchangeRateUsed = rate.getRate();
            pvConverted = pv.multiply(rate.getRate()).setScale(10, RoundingMode.HALF_UP);
        }

        Settlement settlement = Settlement.builder()
                .id(UUID.randomUUID())
                .receivable(settled)
                .baseRate(request.baseRate())
                .presentValue(pv)
                .exchangeRateUsed(exchangeRateUsed)
                .presentValueConverted(pvConverted)
                .paymentCurrency(receivable.getPaymentCurrency())
                .settledAt(LocalDateTime.now())
                .build();

        return toResponse(settlementRepository.save(settlement), strategy.getSpread());
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementResponse findById(UUID id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new SettlementNotFoundException(id));
        PricingStrategy strategy = PricingContext.strategyFor(settlement.getReceivable().getType());
        return toResponse(settlement, strategy.getSpread());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettlementResponse> findByReceivable(UUID receivableId) {
        return settlementRepository.findByReceivableId(receivableId).stream()
                .map(s -> toResponse(s, PricingContext.strategyFor(s.getReceivable().getType()).getSpread()))
                .toList();
    }

    private SettlementResponse toResponse(Settlement s, BigDecimal spread) {
        Receivable r = s.getReceivable();
        return new SettlementResponse(
                s.getId(),
                r.getId(),
                r.getType(),
                r.getAssignor().getId(),
                r.getAssignor().getName(),
                r.getFaceValue(),
                s.getBaseRate(),
                spread,
                s.getPresentValue(),
                r.getAssetCurrency(),
                s.getExchangeRateUsed(),
                s.getPresentValueConverted(),
                s.getPaymentCurrency(),
                s.getSettledAt()
        );
    }
}
