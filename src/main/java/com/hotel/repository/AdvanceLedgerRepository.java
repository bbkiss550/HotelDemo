package com.hotel.repository;

import com.hotel.model.AdvanceLedger;
import com.hotel.model.AdvanceLedgerType;
import com.hotel.model.MonthlyRentBill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvanceLedgerRepository extends JpaRepository<AdvanceLedger, Long> {
    List<AdvanceLedger> findByGuestIdOrderByCreatedAtDescIdDesc(Long guestId);
    List<AdvanceLedger> findByBill(MonthlyRentBill bill);
    boolean existsByBillAndType(MonthlyRentBill bill, AdvanceLedgerType type);
}
