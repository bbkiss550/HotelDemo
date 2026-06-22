package com.hotel.repository;

import com.hotel.model.Receipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    Optional<Receipt> findByPaymentId(Long paymentId);
    long countByReceiptNoStartingWith(String prefix);
}
