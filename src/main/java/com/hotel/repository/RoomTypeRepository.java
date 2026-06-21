package com.hotel.repository;

import com.hotel.model.RoomType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    Optional<RoomType> findByNameIgnoreCase(String name);
    List<RoomType> findAllByOrderByNameAsc();
}
