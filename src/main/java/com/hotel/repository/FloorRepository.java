package com.hotel.repository;

import com.hotel.model.Floor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<Floor, Long> {
    Optional<Floor> findByNumber(Integer number);
    List<Floor> findAllByOrderBySortOrderAscNumberAscNameAsc();
}
