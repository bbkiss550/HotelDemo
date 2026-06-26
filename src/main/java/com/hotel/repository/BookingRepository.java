package com.hotel.repository;

import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    long countByStatus(BookingStatus status);
    List<Booking> findAllByOrderByCheckInDateAscIdDesc();
    List<Booking> findByRoomAndStatusIn(Room room, List<BookingStatus> statuses);

    @Query("""
            select count(b) from Booking b
            left join b.room r
            where (b.roomType = :roomType or (b.roomType is null and r.roomType = :roomType))
              and b.status in :statuses
              and (:excludeId is null or b.id <> :excludeId)
              and b.checkInDate < :checkOutDate
              and (b.checkOutDate is null or b.checkOutDate > :checkInDate)
            """)
    long countOverlappingRoomTypeBookings(RoomType roomType, List<BookingStatus> statuses, LocalDate checkInDate, LocalDate checkOutDate, Long excludeId);

    @Query("""
            select b from Booking b
            where b.status in :statuses
              and b.checkInDate <= :endDate
              and (b.checkOutDate is null or b.checkOutDate >= :startDate)
            order by b.checkInDate asc
            """)
    List<Booking> findActiveBetween(List<BookingStatus> statuses, LocalDate startDate, LocalDate endDate);
}
