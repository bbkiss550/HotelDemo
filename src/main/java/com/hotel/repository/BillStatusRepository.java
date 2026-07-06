package com.hotel.repository;

import com.hotel.model.BillStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillStatusRepository extends JpaRepository<BillStatus, Long> {
    List<BillStatus> findAllByOrderByIdAsc();
}
