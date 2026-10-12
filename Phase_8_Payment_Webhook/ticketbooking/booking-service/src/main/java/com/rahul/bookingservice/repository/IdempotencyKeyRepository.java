package com.rahul.bookingservice.repository;

import com.rahul.bookingservice.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    // Returns 1 if this call claimed the key, 0 if it already exists.
    // If another transaction is inserting the same key, Postgres waits for it to finish first.
    @Modifying
    @Query(value = "INSERT INTO idempotency_keys (user_id, key_value, request_hash, expires_at, created_at) "
            + "VALUES (:userId, :keyValue, :requestHash, :expiresAt, now()) "
            + "ON CONFLICT (user_id, key_value) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId,
                       @Param("keyValue") String keyValue,
                       @Param("requestHash") String requestHash,
                       @Param("expiresAt") LocalDateTime expiresAt);

    Optional<IdempotencyKey> findByUserIdAndKeyValue(Long userId, String keyValue);

    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}