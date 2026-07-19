package com.hotel.repository;

import com.hotel.model.PaymentItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentItemRepository extends JpaRepository<PaymentItem, Long> {
    List<PaymentItem> findByActiveTrueOrderByNameAsc();
    Optional<PaymentItem> findByName(String name);
}
