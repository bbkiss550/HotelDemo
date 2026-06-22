package com.hotel.controller;

import com.hotel.model.PaymentStatus;
import com.hotel.model.RoomStatus;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.ThaiDateFormatter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reports")
public class ReportController {
    private final RoomRepository rooms;
    private final GuestRepository guests;
    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final ThaiDateFormatter thaiDate;

    public ReportController(RoomRepository rooms, GuestRepository guests, PaymentRepository payments, BookingRepository bookings, ThaiDateFormatter thaiDate) {
        this.rooms = rooms;
        this.guests = guests;
        this.payments = payments;
        this.bookings = bookings;
        this.thaiDate = thaiDate;
    }

    @GetMapping
    String index(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("totalRooms", rooms.count());
        model.addAttribute("availableRooms", rooms.countByStatus(RoomStatus.AVAILABLE));
        model.addAttribute("occupiedRooms", rooms.countByStatus(RoomStatus.DAILY_OCCUPIED) + rooms.countByStatus(RoomStatus.MONTHLY_OCCUPIED));
        model.addAttribute("unpaidPayments", payments.countByStatus(PaymentStatus.UNPAID));
        model.addAttribute("todayRevenue", payments.sumPaidBetween(today, today));
        model.addAttribute("monthRevenue", payments.sumPaidBetween(today.withDayOfMonth(1), today));
        model.addAttribute("activeGuests", guests.findByActiveTrueOrderByCheckInDateDescIdDesc());
        model.addAttribute("bookings", bookings.findAllByOrderByCheckInDateAscIdDesc());
        return "reports/index";
    }

    @GetMapping("/payments.csv")
    void exportPayments(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=payments.csv");
        response.getWriter().println("date,room,guest,type,method,status,amount");
        for (var p : payments.findAllByOrderByPaymentDateDescIdDesc()) {
            response.getWriter().printf("%s,%s,%s,%s,%s,%s,%s%n",
                    thaiDate.format(p.getPaymentDate()),
                    p.getRoom() == null ? "" : p.getRoom().getRoomNumber(),
                    p.getGuest() == null ? "" : p.getGuest().getFullName(),
                    p.getPaymentType(),
                    p.getPaymentMethod(),
                    p.getStatus(),
                    p.getAmount());
        }
    }
}
