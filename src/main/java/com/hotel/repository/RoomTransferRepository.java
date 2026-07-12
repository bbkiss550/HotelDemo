package com.hotel.repository;

import com.hotel.model.Guest;
import com.hotel.model.RoomTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomTransferRepository extends JpaRepository<RoomTransfer, Long> {
    List<RoomTransfer> findByGuestOrderByTransferDateDescIdDesc(Guest guest);
}
