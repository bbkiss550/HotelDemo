package com.hotel.repository;

import com.hotel.model.MonthlyRentBill;
import com.hotel.model.Room;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonthlyRentBillRepository extends JpaRepository<MonthlyRentBill, Long> {
    List<MonthlyRentBill> findAllByOrderByBillingYearDescBillingMonthDescIdDesc();
    List<MonthlyRentBill> findByIssueDateBetweenOrderByIssueDateDescIdDesc(java.time.LocalDate startDate, java.time.LocalDate endDate);
    List<MonthlyRentBill> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(LocalDateTime startDateTime, LocalDateTime endDateTime);
    Optional<MonthlyRentBill> findTopByBillNumberStartingWithOrderByBillNumberDesc(String prefix);
    Optional<MonthlyRentBill> findByBillNumber(String billNumber);
    boolean existsByBillNumber(String billNumber);
    Optional<MonthlyRentBill> findByRoomAndBillingMonthAndBillingYear(Room room, Integer billingMonth, Integer billingYear);
    List<MonthlyRentBill> findByBillingMonthAndBillingYear(Integer billingMonth, Integer billingYear);
    List<MonthlyRentBill> findByStatusIdInOrderByDueDateAscIdAsc(List<Long> statusIds);
    @Query("""
            select b from MonthlyRentBill b
            where b.room = :room
              and (b.billingYear < :billingYear or (b.billingYear = :billingYear and b.billingMonth < :billingMonth))
            order by b.billingYear desc, b.billingMonth desc
            """)
    List<MonthlyRentBill> findPreviousBill(@Param("room") Room room, @Param("billingMonth") Integer billingMonth, @Param("billingYear") Integer billingYear);
}
