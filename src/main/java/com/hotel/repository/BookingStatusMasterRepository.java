package com.hotel.repository;

import com.hotel.model.BookingStatusMaster;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingStatusMasterRepository extends JpaRepository<BookingStatusMaster, Long> {
    Optional<BookingStatusMaster> findByCode(String code);
    List<BookingStatusMaster> findByActiveTrueOrderByIdAsc();
}
