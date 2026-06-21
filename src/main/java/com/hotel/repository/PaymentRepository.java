package com.hotel.repository;

import com.hotel.model.Payment;
import com.hotel.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    long countByStatus(PaymentStatus status);
}
