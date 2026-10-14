package com.rahul.cinemaservice.kafka.publisher;

import com.rahul.cinemaservice.entity.OutboxEvent;
import com.rahul.cinemaservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final String TYPE_HEADER = "__TypeId__";   // the header the consumers read to pick the class

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // No @Transactional on purpose: each save commits by itself, and no connection is held while we wait for Kafka.
    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        for (OutboxEvent row : outboxEventRepository.findTop50ByPublishedFalseOrderByIdAsc()) {
            SendResult<String, String> result;
            try {
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(row.getTopic(), row.getMessageKey(), row.getPayload());
                record.headers().add(TYPE_HEADER, row.getEventType().getBytes(StandardCharsets.UTF_8));
                result = kafkaTemplate.send(record).get(5, TimeUnit.SECONDS);   // wait for the broker's confirmation
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Outbox row {} ({}) not published, will try again: {}", row.getId(), row.getEventType(), ex.toString());
                return;   // stop here, so the rows after it cannot overtake it
            }

            row.setPublished(true);
            row.setPublishedAt(LocalDateTime.now());
            outboxEventRepository.save(row);
            log.info("Published {} for booking {}, partition {}, offset {} (outbox row {})",
                    row.getEventType(), row.getMessageKey(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset(), row.getId());
        }
    }
}