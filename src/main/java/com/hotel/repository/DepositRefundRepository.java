package com.hotel.repository;

import com.hotel.model.DepositRefund;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRefundRepository extends JpaRepository<DepositRefund, Long> {
    @EntityGraph(attributePaths = {"guest", "room", "items"})
    List<DepositRefund> findAllByOrderByRefundDateDescIdDesc();
    @EntityGraph(attributePaths = {"guest", "room", "items"})
    List<DepositRefund> findByRefundDateBetweenOrderByRefundDateDescIdDesc(java.time.LocalDate startDate, java.time.LocalDate endDate);
    @EntityGraph(attributePaths = {"guest", "room", "items"})
    List<DepositRefund> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(LocalDateTime startDateTime, LocalDateTime endDateTime);
    @EntityGraph(attributePaths = {"guest", "room", "items"})
    Optional<DepositRefund> findByRefundNo(String refundNo);
    Optional<DepositRefund> findTopByRefundNoStartingWithOrderByRefundNoDesc(String prefix);
}
