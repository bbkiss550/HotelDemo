package com.hotel.controller;

import com.hotel.model.PaymentStatus;
import com.hotel.model.RoomStatus;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.RoomRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final RoomRepository rooms;
    private final PaymentRepository payments;

    public DashboardController(RoomRepository rooms, PaymentRepository payments) {
        this.rooms = rooms;
        this.payments = payments;
    }

    @GetMapping("/")
    String dashboard(Model model) {
        model.addAttribute("totalRooms", rooms.count());
        model.addAttribute("availableRooms", rooms.countByStatus(RoomStatus.AVAILABLE));
        model.addAttribute("occupiedRooms", rooms.countByStatus(RoomStatus.OCCUPIED));
        model.addAttribute("reservedRooms", rooms.countByStatus(RoomStatus.RESERVED));
        model.addAttribute("unpaidRooms", payments.countByStatus(PaymentStatus.UNPAID));
        return "dashboard";
    }
}
