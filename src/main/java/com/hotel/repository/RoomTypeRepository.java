package com.hotel.repository;

import com.hotel.model.RoomType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    Optional<RoomType> findByNameIgnoreCase(String name);

    @Query("select rt from RoomType rt where lower(rt.name) not in ('standard', 'deluxe') order by rt.name asc")
    List<RoomType> findAllByOrderByNameAsc();
}
