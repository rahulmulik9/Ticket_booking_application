package com.rahul.paymentservice.kafka.publisher;

import com.rahul.paymentservice.entity.OutboxEvent;
import com.rahul.paymentservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private final OutboxEventRepository outboxEventRepository;

    // Only used to turn the event into JSON. Nothing is sent from here.
    private final JacksonJsonSerializer<Object> serializer = new JacksonJsonSerializer<>();

    // MANDATORY: this must run inside the caller's transaction. Called without one, it throws.
    // That is the whole point: the outbox row and the booking change commit together.
    @Transactional(propagation = Propagation.MANDATORY)
    public void write(String topic, String key, String eventType, Object event) {
        OutboxEvent row = new OutboxEvent();
        row.setTopic(topic);
        row.setMessageKey(key);
        row.setEventType(eventType);
        row.setPayload(new String(serializer.serialize(topic, event), StandardCharsets.UTF_8));
        row.setPublished(false);
        row.setCreatedAt(LocalDateTime.now());
        outboxEventRepository.save(row);
    }
}