package com.hotel.repository;

import com.hotel.model.AdvanceLedger;
import com.hotel.model.MonthlyRentBill;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdvanceLedgerRepository extends JpaRepository<AdvanceLedger, Long> {
    List<AdvanceLedger> findByGuestIdOrderByCreatedAtDescIdDesc(Long guestId);
    List<AdvanceLedger> findByBill(MonthlyRentBill bill);
    @Query("select count(a) > 0 from AdvanceLedger a where a.bill = :bill and a.typeMaster.code = :type")
    boolean existsByBillAndType(@Param("bill") MonthlyRentBill bill, @Param("type") String type);
}
