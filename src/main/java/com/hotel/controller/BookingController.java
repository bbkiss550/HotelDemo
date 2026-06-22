package com.hotel.controller;

import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.RoomStatus;
import com.hotel.model.StayType;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.AuditService;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/bookings")
public class BookingController {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final AuditService audit;

    public BookingController(BookingRepository bookings, RoomRepository rooms, AuditService audit) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.audit = audit;
    }

    @GetMapping
    String index(Model model) {
        model.addAttribute("bookings", bookings.findAllByOrderByCheckInDateAscIdDesc());
        model.addAttribute("rooms", rooms.findAllByOrderByRoomNumber());
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("stayTypes", StayType.values());
        model.addAttribute("booking", new Booking());
        return "bookings/index";
    }

    @PostMapping
    String save(@ModelAttribute Booking booking, @RequestParam Long roomId) {
        var room = rooms.findById(roomId).orElseThrow();
        booking.setRoom(room);
        if (booking.getBookingDate() == null) {
            booking.setBookingDate(LocalDate.now());
        }
        bookings.save(booking);
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            room.setStatus(RoomStatus.RESERVED);
            rooms.save(room);
        }
        audit.record("BOOKING", "Room " + room.getRoomNumber() + " customer " + booking.getCustomerName());
        return "redirect:/bookings";
    }

    @PostMapping("/{id}/cancel")
    String cancel(@PathVariable Long id) {
        var booking = bookings.findById(id).orElseThrow();
        booking.setStatus(BookingStatus.CANCELLED);
        bookings.save(booking);
        var room = booking.getRoom();
        if (room.getStatus() == RoomStatus.RESERVED) {
            room.setStatus(RoomStatus.AVAILABLE);
            rooms.save(room);
        }
        audit.record("BOOKING_CANCEL", "Room " + room.getRoomNumber() + " customer " + booking.getCustomerName());
        return "redirect:/bookings";
    }
}
