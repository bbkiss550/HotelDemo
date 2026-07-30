package com.hotel.controller;

import com.hotel.model.LookupCodes;
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
        model.addAttribute("availableRooms", rooms.countByStatus(LookupCodes.AVAILABLE));
        model.addAttribute("dailyRooms", rooms.countByStatus(LookupCodes.DAILY_OCCUPIED));
        model.addAttribute("monthlyRooms", rooms.countByStatus(LookupCodes.MONTHLY_OCCUPIED));
        model.addAttribute("reservedRooms", rooms.countByStatus(LookupCodes.RESERVED));
        model.addAttribute("unpaidRooms", payments.countByStatusId(2L));
        model.addAttribute("todayRevenue", payments.sumPaidBetween(today, today));
        model.addAttribute("monthRevenue", payments.sumPaidBetween(today.withDayOfMonth(1), today));
        model.addAttribute("activeBookings", bookings.countByStatus(LookupCodes.PENDING));
        return "dashboard";
    }
}
