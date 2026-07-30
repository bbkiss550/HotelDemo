package com.hotel.repository;

import com.hotel.model.Payment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    long countByStatusId(Long statusId);
    List<Payment> findAllByOrderByPaymentDateDescIdDesc();
    List<Payment> findByPaymentDateBetweenOrderByPaymentDateDescIdDesc(LocalDate startDate, LocalDate endDate);
    @Query("""
            select p from Payment p
            left join p.reciept r
            where p.createdAt >= :startDateTime
              and p.createdAt < :endDateTime
            order by
              case when r.recieptNo is null then 1 else 0 end,
              r.recieptNo asc,
              p.paymentDate asc,
              p.id asc
            """)
    List<Payment> findByCreatedAtBetweenOrderByReceiptNoAsc(LocalDateTime startDateTime, LocalDateTime endDateTime);
    @Query("""
            select p from Payment p
            left join p.reciept r
            order by
              case when r.recieptNo is null then 1 else 0 end,
              r.recieptNo desc,
              p.paymentDate desc,
              p.id desc
            """)
    List<Payment> findAllOrderByReceiptNoDesc();
    List<Payment> findByGuestIdOrderByPaymentDateDescIdDesc(Long guestId);
    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.statusMaster.code = 'PAID'
              and p.paymentDate between :startDate and :endDate
    """)
    BigDecimal sumPaidBetween(LocalDate startDate, LocalDate endDate);
    Optional<Payment> findFirstByBookingIdOrderByIdAsc(Long bookingId);
}
