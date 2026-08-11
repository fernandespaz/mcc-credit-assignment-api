package com.srm.mcc.credit.application.usecase;

import com.srm.mcc.credit.application.dto.request.CreateReceivableRequest;
import com.srm.mcc.credit.application.dto.response.PresentValueResponse;
import com.srm.mcc.credit.application.dto.response.ReceivableResponse;
import com.srm.mcc.credit.domain.entity.Assignor;
import com.srm.mcc.credit.domain.entity.ExchangeRate;
import com.srm.mcc.credit.domain.entity.Receivable;
import com.srm.mcc.credit.domain.exception.AssignorNotFoundException;
import com.srm.mcc.credit.domain.exception.ExchangeRateNotFoundException;
import com.srm.mcc.credit.domain.exception.ReceivableNotFoundException;
import com.srm.mcc.credit.domain.port.in.ManageReceivableUseCase;
import com.srm.mcc.credit.domain.port.out.AssignorRepositoryPort;
import com.srm.mcc.credit.domain.port.out.ExchangeRateRepositoryPort;
import com.srm.mcc.credit.domain.port.out.ReceivableRepositoryPort;
import com.srm.mcc.credit.domain.service.pricing.PricingContext;
import com.srm.mcc.credit.domain.service.pricing.PricingStrategy;
import com.srm.mcc.credit.domain.enums.SettlementStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceivableApplicationService implements ManageReceivableUseCase {

    private final ReceivableRepositoryPort receivableRepository;
    private final AssignorRepositoryPort assignorRepository;
    private final ExchangeRateRepositoryPort exchangeRateRepository;

    @Override
    @Transactional
    public ReceivableResponse create(CreateReceivableRequest request) {
        Assignor assignor = assignorRepository.findById(request.assignorId())
                .orElseThrow(() -> new AssignorNotFoundException(request.assignorId()));

        Receivable receivable = Receivable.builder()
                .id(UUID.randomUUID())
                .assignor(assignor)
                .type(request.type())
                .faceValue(request.faceValue())
                .assetCurrency(request.assetCurrency())
                .paymentCurrency(request.paymentCurrency())
                .maturityDate(request.maturityDate())
                .termMonths(request.termMonths())
                .status(SettlementStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(receivableRepository.save(receivable));
    }

    @Override
    @Transactional(readOnly = true)
    public ReceivableResponse findById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceivableResponse> findAll() {
        return receivableRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReceivableResponse> findByAssignor(UUID assignorId) {
        return receivableRepository.findByAssignorId(assignorId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PresentValueResponse simulate(UUID receivableId, BigDecimal baseRate) {
        Receivable receivable = findOrThrow(receivableId);
        PricingStrategy strategy = PricingContext.strategyFor(receivable.getType());
        BigDecimal pv = strategy.calculatePresentValue(receivable.getFaceValue(), baseRate, receivable.getTermMonths());

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

        return new PresentValueResponse(
                receivable.getType(),
                receivable.getFaceValue(),
                receivable.getAssetCurrency(),
                baseRate,
                strategy.getSpread(),
                receivable.getTermMonths(),
                pv,
                exchangeRateUsed,
                pvConverted,
                receivable.getPaymentCurrency()
        );
    }

    private Receivable findOrThrow(UUID id) {
        return receivableRepository.findById(id)
                .orElseThrow(() -> new ReceivableNotFoundException(id));
    }

    private ReceivableResponse toResponse(Receivable r) {
        return new ReceivableResponse(
                r.getId(),
                r.getAssignor().getId(),
                r.getAssignor().getName(),
                r.getType(),
                r.getFaceValue(),
                r.getAssetCurrency(),
                r.getPaymentCurrency(),
                r.isCrossCurrency(),
                r.getMaturityDate(),
                r.getTermMonths(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}
