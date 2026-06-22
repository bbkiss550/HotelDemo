package com.hotel.repository;

import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.Room;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    long countByStatus(BookingStatus status);
    List<Booking> findAllByOrderByCheckInDateAscIdDesc();
    List<Booking> findByRoomAndStatusIn(Room room, List<BookingStatus> statuses);

    @Query("""
            select b from Booking b
            where b.status in :statuses
              and b.checkInDate <= :endDate
              and (b.checkOutDate is null or b.checkOutDate >= :startDate)
            order by b.checkInDate asc
            """)
    List<Booking> findActiveBetween(List<BookingStatus> statuses, LocalDate startDate, LocalDate endDate);
}
