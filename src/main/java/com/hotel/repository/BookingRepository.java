package com.hotel.repository;

import com.hotel.model.Booking;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("select count(b) from Booking b where b.statusMaster.code = :status")
    long countByStatus(@Param("status") String status);
    List<Booking> findAllByOrderByCheckInDateAscIdDesc();
    List<Booking> findByBookingDateBetweenOrderByBookingNumberAscIdAsc(LocalDate startDate, LocalDate endDate);
    @Query("select b from Booking b where b.room = :room and b.statusMaster.code in :statuses")
    List<Booking> findByRoomAndStatusIn(@Param("room") Room room, @Param("statuses") List<String> statuses);
    @Query("select b from Booking b where b.room is null and b.statusMaster.code = :status order by b.checkInDate asc, b.id desc")
    List<Booking> findByRoomIsNullAndStatusOrderByCheckInDateAscIdDesc(@Param("status") String status);
    @Query("select b from Booking b where b.room = :room and b.statusMaster.code = :status order by b.checkInDate desc, b.id desc")
    java.util.Optional<Booking> findTopByRoomAndStatusOrderByCheckInDateDescIdDesc(@Param("room") Room room, @Param("status") String status);
    Optional<Booking> findByBookingNumber(String bookingNumber);

    @Query("select max(b.bookingNumber) from Booking b where b.bookingNumber like concat(:prefix, '%')")
    String findMaxBookingNumberByPrefix(@Param("prefix") String prefix);

    @Query("""
            select b from Booking b
            left join b.room r
            left join b.roomType rt
            where (:customerName = '' or lower(b.customerName) like concat('%', lower(:customerName), '%'))
              and (:phone = '' or b.phone like concat('%', :phone, '%'))
              and (:checkInDateFilter = false or b.checkInDate = :checkInDate)
              and (:roomTypeFilter = false
                   or (rt is not null and rt.id = :roomTypeId)
                   or (rt is null and r.roomType.id = :roomTypeId))
              and (:stayTypeFilter = false or b.stayTypeMaster.code = :stayType)
            order by b.bookingNumber desc nulls last, b.id desc
            """)
    List<Booking> searchBookings(@Param("customerName") String customerName,
                                 @Param("phone") String phone,
                                 @Param("checkInDateFilter") boolean checkInDateFilter,
                                 @Param("checkInDate") LocalDate checkInDate,
                                 @Param("roomTypeFilter") boolean roomTypeFilter,
                                 @Param("roomTypeId") Long roomTypeId,
                                 @Param("stayTypeFilter") boolean stayTypeFilter,
                                 @Param("stayType") String stayType);

    @Query("""
            select count(b) from Booking b
            left join b.room r
            where (b.roomType = :roomType or (b.roomType is null and r.roomType = :roomType))
              and b.statusMaster.code in :statuses
              and (:excludeId is null or b.id <> :excludeId)
              and b.checkInDate < :checkOutDate
              and (b.checkOutDate is null or b.checkOutDate > :checkInDate)
            """)
    long countOverlappingRoomTypeBookings(RoomType roomType, List<String> statuses, LocalDate checkInDate, LocalDate checkOutDate, Long excludeId);

    @Query("""
            select count(b) from Booking b
            where b.room = :room
              and b.statusMaster.code in :statuses
              and (:excludeId is null or b.id <> :excludeId)
              and b.checkInDate < :checkOutDate
              and (b.checkOutDate is null or b.checkOutDate > :checkInDate)
            """)
    long countOverlappingRoomBookings(Room room, List<String> statuses, LocalDate checkInDate, LocalDate checkOutDate, Long excludeId);

    @Query("""
            select b from Booking b
            where b.statusMaster.code in :statuses
              and b.checkInDate <= :endDate
              and (b.checkOutDate is null or b.checkOutDate >= :startDate)
            order by b.checkInDate asc
            """)
    List<Booking> findActiveBetween(List<String> statuses, LocalDate startDate, LocalDate endDate);
}
