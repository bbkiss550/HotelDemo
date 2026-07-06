package com.hotel.repository;

import com.hotel.model.DepositRefund;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRefundRepository extends JpaRepository<DepositRefund, Long> {
    @EntityGraph(attributePaths = {"guest", "room", "items"})
    List<DepositRefund> findAllByOrderByRefundDateDescIdDesc();
    Optional<DepositRefund> findTopByRefundNoStartingWithOrderByRefundNoDesc(String prefix);
}
