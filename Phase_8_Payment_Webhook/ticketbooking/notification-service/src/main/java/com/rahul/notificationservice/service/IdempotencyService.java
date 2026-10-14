package com.rahul.notificationservice.service;

import com.rahul.notificationservice.repository.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final ProcessedMessageRepository processedMessageRepository;
    private final TransactionTemplate transactionTemplate;

    // A cheap early check, used before slow work such as a gateway call.
    public boolean isProcessed(String eventId) {
        return processedMessageRepository.existsById(eventId);
    }

    // true = first time, false = already processed.
    // MANDATORY: claims said : I must be called inside a transaction that someone else already started. I won’t start my own
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(String eventId) {
        return processedMessageRepository.insertIfAbsent(eventId) == 1;
    }

    // Claim and work in one transaction. If the work throws, the claim rolls back too,
    // so the Kafka retry runs the work again.
    //TransactionTemplate is Spring’s way of running a block of code inside a transaction manually, without @Transactional.
    public void runOnce(String eventId, Runnable work) {
        boolean ran = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            if (!claim(eventId)) {
                return false;
            }
            work.run();
            return true;
        }));
        if (!ran) {
            log.info("Event {} already processed, skipped", eventId);
        }
    }
}