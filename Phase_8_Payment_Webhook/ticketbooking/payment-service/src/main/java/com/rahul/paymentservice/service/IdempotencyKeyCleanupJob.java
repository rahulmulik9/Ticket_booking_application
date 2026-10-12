package com.rahul.paymentservice.service;

import com.rahul.paymentservice.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyKeyCleanupJob {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Scheduled(fixedDelay = 3_600_000)   // every hour
    @Transactional
    public void deleteExpiredKeys() {
        int deleted = idempotencyKeyRepository.deleteExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Deleted {} expired idempotency keys", deleted);
        }
    }
}