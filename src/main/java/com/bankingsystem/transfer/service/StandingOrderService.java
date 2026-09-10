package com.bankingsystem.transfer.service;

import com.bankingsystem.transfer.domain.StandingOrder;
import com.bankingsystem.transfer.dto.StandingOrderRequest;
import com.bankingsystem.transfer.dto.StandingOrderResponse;
import com.bankingsystem.transfer.exception.StandingOrderNotFoundException;
import com.bankingsystem.transfer.repository.StandingOrderRepository;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StandingOrderService {

    private final StandingOrderRepository standingOrderRepository;

    public StandingOrderService(StandingOrderRepository standingOrderRepository) {
        this.standingOrderRepository = standingOrderRepository;
    }

    public StandingOrderResponse create(StandingOrderRequest request) {
        if (!CronExpression.isValidExpression(request.cronExpression())) {
            throw new IllegalArgumentException("cronExpression is not a valid cron expression: " + request.cronExpression());
        }
        // Without this, a bad amount creates an order that then fails every
        // scheduled run forever (recordFailure never advances lastExecutedAt).
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        StandingOrder order = new StandingOrder(
                UUID.randomUUID().toString(),
                request.sourceIban(),
                request.destinationIban(),
                request.amount(),
                request.cronExpression(),
                Instant.now()
        );
        return StandingOrderResponse.from(standingOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<StandingOrderResponse> listActive() {
        return standingOrderRepository.findByActiveTrue().stream()
                .map(StandingOrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StandingOrderResponse getById(String id) {
        return StandingOrderResponse.from(findOrThrow(id));
    }

    public void cancel(String id) {
        StandingOrder order = findOrThrow(id);
        order.cancel();
        standingOrderRepository.save(order);
    }

    private StandingOrder findOrThrow(String id) {
        return standingOrderRepository.findById(id)
                .orElseThrow(() -> new StandingOrderNotFoundException(id));
    }
}
