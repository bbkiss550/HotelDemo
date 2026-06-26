package com.hotel.controller;

import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.RoomStatus;
import com.hotel.model.StayType;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.RoomTypeRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.AuditService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bookings")
public class BookingController {
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final RoomTypeRepository roomTypes;
    private final AuditService audit;

    public BookingController(BookingRepository bookings, RoomRepository rooms, RoomTypeRepository roomTypes, AuditService audit) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.roomTypes = roomTypes;
        this.audit = audit;
    }

    @GetMapping
    String index(Model model) {
        addBookingPageData(model);
        return "bookings/index";
    }

    @GetMapping("/content")
    String content(Model model) {
        addBookingPageData(model);
        return "bookings/index :: bookingWorkspace";
    }

    private void addBookingPageData(Model model) {
        var roomTypeList = roomTypes.findAllByOrderByNameAsc();
        var availableRoomCounts = new HashMap<Long, Long>();
        var today = LocalDate.now();
        var tomorrow = today.plusDays(1);
        for (var roomType : roomTypeList) {
            availableRoomCounts.put(roomType.getId(), countAvailableRooms(roomType, today, tomorrow, null));
        }
        model.addAttribute("bookings", bookings.findAllByOrderByCheckInDateAscIdDesc());
        model.addAttribute("roomTypes", roomTypeList);
        model.addAttribute("availableRoomCounts", availableRoomCounts);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("stayTypes", StayType.values());
        var booking = new Booking();
        booking.setNationality("ไทย");
        model.addAttribute("booking", booking);
    }

    @PostMapping
    Object save(@ModelAttribute Booking booking, @RequestParam Long roomTypeId,
                @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                RedirectAttributes redirect) {
        boolean isNew = booking.getId() == null;
        var roomType = roomTypes.findById(roomTypeId).orElseThrow();
        Booking savedBooking = isNew ? new Booking() : bookings.findById(booking.getId()).orElseThrow();
        var checkInDate = booking.getCheckInDate();
        var checkOutDate = booking.getCheckOutDate();
        if (checkInDate != null && (checkOutDate == null || checkInDate.isAfter(checkOutDate))) {
            checkOutDate = checkInDate.plusDays(1);
        }
        if (checkInDate != null && checkOutDate != null && countAvailableRooms(roomType, checkInDate, checkOutDate, savedBooking.getId()) <= 0) {
            if (isAjax(requestedWith)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "type", "warning",
                        "message", "ไม่มีห้องว่างสำหรับประเภทห้องและช่วงวันที่เลือก"
                ));
            }
            redirect.addFlashAttribute("error", "ไม่มีห้องว่างสำหรับประเภทห้องและช่วงวันที่เลือก");
            redirect.addFlashAttribute("flashType", "warning");
            return "redirect:/bookings";
        }
        if (!isNew && savedBooking.getRoom() != null && savedBooking.getRoom().getStatus() == RoomStatus.RESERVED) {
            var previousRoom = savedBooking.getRoom();
            previousRoom.setStatus(RoomStatus.AVAILABLE);
            rooms.save(previousRoom);
        }

        savedBooking.setCustomerName(booking.getCustomerName());
        savedBooking.setPhone(booking.getPhone());
        savedBooking.setIdCard(booking.getIdCard());
        savedBooking.setNationality(booking.getNationality());
        savedBooking.setCheckInDate(checkInDate);
        savedBooking.setCheckOutDate(checkOutDate);
        savedBooking.setStayType(booking.getStayType());
        savedBooking.setDepositAmount(booking.getDepositAmount());
        savedBooking.setStatus(booking.getStatus());
        savedBooking.setNote(booking.getNote());
        savedBooking.setRoomType(roomType);
        savedBooking.setRoom(null);
        if (savedBooking.getBookingDate() == null) {
            savedBooking.setBookingDate(LocalDate.now());
        }
        bookings.save(savedBooking);
        audit.record("BOOKING", "Room type " + roomType.getName() + " customer " + savedBooking.getCustomerName());
        if (isAjax(requestedWith)) {
            return ResponseEntity.ok(Map.of(
                    "type", isNew ? "success" : "edit",
                    "message", (isNew ? "บันทึกการจอง" : "แก้ไขการจอง") + "เรียบร้อย"
            ));
        }
        redirect.addFlashAttribute("message", (isNew ? "บันทึกการจอง" : "แก้ไขการจอง") + "เรียบร้อย");
        redirect.addFlashAttribute("flashType", isNew ? "success" : "edit");
        return "redirect:/bookings";
    }

    @GetMapping("/availability")
    @ResponseBody
    Map<String, Long> availability(@RequestParam Long roomTypeId,
                                   @RequestParam LocalDate checkInDate,
                                   @RequestParam LocalDate checkOutDate,
                                   @RequestParam(required = false) Long excludeId) {
        if (checkInDate.isAfter(checkOutDate)) {
            checkOutDate = checkInDate.plusDays(1);
        }
        var roomType = roomTypes.findById(roomTypeId).orElseThrow();
        return Map.of("available", countAvailableRooms(roomType, checkInDate, checkOutDate, excludeId));
    }

    @PostMapping("/{id}/cancel")
    Object cancel(@PathVariable Long id,
                  @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                  RedirectAttributes redirect) {
        var booking = bookings.findById(id).orElseThrow();
        booking.setStatus(BookingStatus.CANCELLED);
        bookings.save(booking);
        var room = booking.getRoom();
        if (room != null && room.getStatus() == RoomStatus.RESERVED) {
            room.setStatus(RoomStatus.AVAILABLE);
            rooms.save(room);
        }
        var bookingTarget = booking.getRoomType() != null ? booking.getRoomType().getName() : room != null ? room.getRoomNumber() : "";
        audit.record("BOOKING_CANCEL", "Booking " + bookingTarget + " customer " + booking.getCustomerName());
        if (isAjax(requestedWith)) {
            return ResponseEntity.ok(Map.of(
                    "type", "delete",
                    "message", "ยกเลิกการจองเรียบร้อย"
            ));
        }
        redirect.addFlashAttribute("message", "ยกเลิกการจองเรียบร้อย");
        redirect.addFlashAttribute("flashType", "delete");
        return "redirect:/bookings";
    }

    private boolean isAjax(String requestedWith) {
        return "XMLHttpRequest".equals(requestedWith);
    }

    private long countAvailableRooms(com.hotel.model.RoomType roomType, LocalDate checkInDate, LocalDate checkOutDate, Long excludeId) {
        long availableByRoomStatus = rooms.countByRoomTypeAndStatusIn(roomType, List.of(RoomStatus.AVAILABLE, RoomStatus.RESERVED));
        if (checkInDate == null || checkOutDate == null) {
            return availableByRoomStatus;
        }
        long overlappingBookings = bookings.countOverlappingRoomTypeBookings(
                roomType,
                List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN),
                checkInDate,
                checkOutDate,
                excludeId
        );
        return Math.max(0, availableByRoomStatus - overlappingBookings);
    }
}
