package com.rahul.cinemaservice.security;

import com.rahul.bookingservice.repository.ProcessedMessageRepository;
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
    // MANDATORY: the claim must commit or roll back together with the work.
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean claim(String eventId) {
        return processedMessageRepository.insertIfAbsent(eventId) == 1;
    }

    // Claim and work in one transaction. If the work throws, the claim rolls back too,
    // so the Kafka retry runs the work again.
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