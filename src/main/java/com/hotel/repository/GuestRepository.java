package com.hotel.repository;

import com.hotel.model.Guest;
import com.hotel.model.Room;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GuestRepository extends JpaRepository<Guest, Long> {
    @Query("""
            select g from Guest g
            where lower(g.fullName) like lower(concat('%', :q, '%'))
               or lower(g.phone) like lower(concat('%', :q, '%'))
               or lower(g.room.roomNumber) like lower(concat('%', :q, '%'))
            order by g.fullName
            """)
    List<Guest> search(String q);

    Optional<Guest> findTopByRoomOrderByCheckInDateDescIdDesc(Room room);
    Optional<Guest> findTopByRoomAndActiveTrueOrderByCheckInDateDescIdDesc(Room room);
    List<Guest> findByActiveTrueOrderByCheckInDateDescIdDesc();
    @Query("select count(g) from Guest g where g.active = true and g.stayTypeMaster.code = :stayType and g.checkOutDate < :date")
    long countByActiveTrueAndStayTypeAndCheckOutDateBefore(@Param("stayType") String stayType, @Param("date") LocalDate date);
    @Query("select g from Guest g where g.active = true and g.stayTypeMaster.code = :stayType and g.checkOutDate < :date order by g.checkOutDate asc, g.id asc")
    List<Guest> findTop5ByActiveTrueAndStayTypeAndCheckOutDateBeforeOrderByCheckOutDateAscIdAsc(@Param("stayType") String stayType, @Param("date") LocalDate date);
}
