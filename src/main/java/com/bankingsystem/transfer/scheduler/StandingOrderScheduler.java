package com.bankingsystem.transfer.scheduler;

import com.bankingsystem.transfer.domain.StandingOrder;
import com.bankingsystem.transfer.repository.StandingOrderRepository;
import com.bankingsystem.transfer.service.TransferService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Polls active standing orders every {@code app.standing-order.poll-interval-ms}
 * and fires any that are due. {@code @SchedulerLock} keeps only one app
 * instance running this at a time.
 *
 * <p>If a due order's transfer fails, it's logged and {@code lastExecutedAt}
 * is NOT advanced, so the order stays "due" and gets retried next poll
 * instead of silently skipped.
 */
@Component
public class StandingOrderScheduler {

    private static final Logger log = LogManager.getLogger(StandingOrderScheduler.class);

    private final StandingOrderRepository standingOrderRepository;
    private final TransferService transferService;

    public StandingOrderScheduler(StandingOrderRepository standingOrderRepository, TransferService transferService) {
        this.standingOrderRepository = standingOrderRepository;
        this.transferService = transferService;
    }

    @Scheduled(fixedDelayString = "${app.standing-order.poll-interval-ms}")
    @SchedulerLock(name = "standingOrderPoll", lockAtMostFor = "PT5M", lockAtLeastFor = "PT5S")
    public void pollAndExecuteDueOrders() {
        List<StandingOrder> activeOrders = standingOrderRepository.findByActiveTrue();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        for (StandingOrder order : activeOrders) {
            try {
                if (isDue(order, now)) {
                    log.info("Standing order {} is due -- executing", order.getId());
                    transferService.executeForStandingOrder(order);
                    order.recordSuccess(Instant.now());
                    standingOrderRepository.save(order);
                }
            } catch (Exception ex) {
                log.error("Standing order {} failed to execute ({}); it will be retried on the next poll, not skipped",
                        order.getId(), ex.getMessage());
                order.recordFailure(ex.getMessage());
                standingOrderRepository.save(order);
            }
        }
    }

    private boolean isDue(StandingOrder order, LocalDateTime now) {
        CronExpression cron = CronExpression.parse(order.getCronExpression());
        Instant reference = order.getLastExecutedAt() != null ? order.getLastExecutedAt() : order.getCreatedAt();
        LocalDateTime referenceLdt = LocalDateTime.ofInstant(reference, ZoneOffset.UTC);
        LocalDateTime nextFireTime = cron.next(referenceLdt);
        return nextFireTime != null && !nextFireTime.isAfter(now);
    }
}
