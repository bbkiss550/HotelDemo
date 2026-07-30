package com.hotel.repository;

import com.hotel.model.Floor;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    @Query("select count(r) from Room r where r.statusMaster.code = :status")
    long countByStatus(@Param("status") String status);
    long countByFloor(Floor floor);
    long countByRoomType(RoomType roomType);
    @Query("select count(r) from Room r where r.roomType = :roomType and r.statusMaster.code = :status")
    long countByRoomTypeAndStatus(@Param("roomType") RoomType roomType, @Param("status") String status);
    @Query("select count(r) from Room r where r.roomType = :roomType and r.statusMaster.code in :statuses")
    long countByRoomTypeAndStatusIn(@Param("roomType") RoomType roomType, @Param("statuses") List<String> statuses);
    @Query("select r from Room r where r.roomType = :roomType and r.statusMaster.code = :status order by r.roomNumber")
    List<Room> findByRoomTypeAndStatusOrderByRoomNumber(@Param("roomType") RoomType roomType, @Param("status") String status);
    @Query("select r from Room r where r.statusMaster.code = :status order by r.roomNumber")
    List<Room> findByStatusOrderByRoomNumber(@Param("status") String status);
    List<Room> findByRoomNumberContainingIgnoreCaseOrderByRoomNumber(String roomNumber);
    List<Room> findAllByOrderByRoomNumber();
    List<Room> findByFloorOrderByRoomNumber(Floor floor);
    List<Room> findByFloorAndRoomNumberContainingIgnoreCaseOrderByRoomNumber(Floor floor, String roomNumber);
}
