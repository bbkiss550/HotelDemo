package com.hotel.repository;

import com.hotel.model.PaymentDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentDetailRepository extends JpaRepository<PaymentDetail, Long> {
    List<PaymentDetail> findByPaymentIdOrderBySortOrderAscIdAsc(Long paymentId);
    void deleteByPaymentId(Long paymentId);
}
