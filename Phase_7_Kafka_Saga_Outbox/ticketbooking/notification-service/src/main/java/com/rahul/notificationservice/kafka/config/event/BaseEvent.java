package com.rahul.notificationservice.kafka.config.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseEvent {
    private String eventId = UUID.randomUUID().toString();   // used in Step 8 to skip duplicates
    private Instant occurredAt = Instant.now();
}