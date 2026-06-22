package com.hotel.controller;

import com.hotel.model.BookingStatus;
import com.hotel.model.PaymentStatus;
import com.hotel.model.RoomStatus;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.RoomRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final RoomRepository rooms;
    private final PaymentRepository payments;
    private final BookingRepository bookings;

    public DashboardController(RoomRepository rooms, PaymentRepository payments, BookingRepository bookings) {
        this.rooms = rooms;
        this.payments = payments;
        this.bookings = bookings;
    }

    @GetMapping("/")
    String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("totalRooms", rooms.count());
        model.addAttribute("availableRooms", rooms.countByStatus(RoomStatus.AVAILABLE));
        model.addAttribute("dailyRooms", rooms.countByStatus(RoomStatus.DAILY_OCCUPIED));
        model.addAttribute("monthlyRooms", rooms.countByStatus(RoomStatus.MONTHLY_OCCUPIED));
        model.addAttribute("reservedRooms", rooms.countByStatus(RoomStatus.RESERVED));
        model.addAttribute("unpaidRooms", payments.countByStatus(PaymentStatus.UNPAID));
        model.addAttribute("todayRevenue", payments.sumPaidBetween(today, today));
        model.addAttribute("monthRevenue", payments.sumPaidBetween(today.withDayOfMonth(1), today));
        model.addAttribute("activeBookings", bookings.countByStatus(BookingStatus.CONFIRMED));
        return "dashboard";
    }
}
