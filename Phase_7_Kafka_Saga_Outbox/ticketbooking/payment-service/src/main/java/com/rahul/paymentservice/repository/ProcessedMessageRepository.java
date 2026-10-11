package com.rahul.paymentservice.repository;

import com.rahul.paymentservice.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {

    // Returns 1 if this call added the row, 0 if the event was already there.
    // The database decides, so two threads cannot both win.
    @Modifying
    @Query(value = "INSERT INTO processed_messages (message_id, processed_at) VALUES (:messageId, now()) "
            + "ON CONFLICT (message_id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("messageId") String messageId);
}