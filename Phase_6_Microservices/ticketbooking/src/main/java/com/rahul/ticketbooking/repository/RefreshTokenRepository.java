package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("select r from RefreshToken r join fetch r.user where r.tokenHash = :hash")
    Optional<RefreshToken> findByTokenHashWithUser(@Param("hash") String hash);

    // returns 1 if this call revoked it, 0 if it was already revoked or expired
    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.id = :id and r.revoked = false and r.expiresAt > :now")
    int revokeIfActive(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.tokenHash = :hash")
    int revokeByTokenHash(@Param("hash") String hash);
}