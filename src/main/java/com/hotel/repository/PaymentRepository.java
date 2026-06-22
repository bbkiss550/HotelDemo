package com.hotel.repository;

import com.hotel.model.Payment;
import com.hotel.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    long countByStatus(PaymentStatus status);
    List<Payment> findAllByOrderByPaymentDateDescIdDesc();
    List<Payment> findByGuestIdOrderByPaymentDateDescIdDesc(Long guestId);

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.status = com.hotel.model.PaymentStatus.PAID
              and p.paymentDate between :startDate and :endDate
            """)
    BigDecimal sumPaidBetween(LocalDate startDate, LocalDate endDate);
}
