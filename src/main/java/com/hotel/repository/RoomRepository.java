package com.hotel.repository;

import com.hotel.model.Floor;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {
    long countByStatus(RoomStatus status);
    long countByFloor(Floor floor);
    long countByRoomType(RoomType roomType);
    List<Room> findByRoomNumberContainingIgnoreCaseOrderByRoomNumber(String roomNumber);
    List<Room> findAllByOrderByRoomNumber();
    List<Room> findByFloorOrderByRoomNumber(Floor floor);
    List<Room> findByFloorAndRoomNumberContainingIgnoreCaseOrderByRoomNumber(Floor floor, String roomNumber);
}
