package com.rahul.paymentservice.repository;

import com.rahul.paymentservice.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Long> {
}