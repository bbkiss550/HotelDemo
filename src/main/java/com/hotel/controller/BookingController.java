package com.hotel.controller;

import com.hotel.model.Booking;
import com.hotel.model.Payment;
import com.hotel.model.LookupCodes;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.RoomTypeRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.AppSettingService;
import com.hotel.service.AuditService;
import com.hotel.service.RecieptRecordService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.transaction.annotation.Transactional;
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
    private final PaymentRepository payments;
    private final AppSettingService settings;
    private final RecieptRecordService recieptRecordService;
    private final AuditService audit;

    public BookingController(BookingRepository bookings, RoomRepository rooms, RoomTypeRepository roomTypes, PaymentRepository payments, AppSettingService settings, RecieptRecordService recieptRecordService, AuditService audit) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.roomTypes = roomTypes;
        this.payments = payments;
        this.settings = settings;
        this.recieptRecordService = recieptRecordService;
        this.audit = audit;
    }

    @GetMapping
    String index(Model model,
                 @RequestParam(required = false) String customerName,
                 @RequestParam(required = false) String phone,
                 @RequestParam(required = false) LocalDate checkInDate,
                 @RequestParam(required = false) Long roomTypeId,
                 @RequestParam(required = false) String stayType) {
        addBookingPageData(model, customerName, phone, checkInDate, roomTypeId, stayType);
        return "bookings/index";
    }

    @GetMapping("/content")
    String content(Model model,
                   @RequestParam(required = false) String customerName,
                   @RequestParam(required = false) String phone,
                   @RequestParam(required = false) LocalDate checkInDate,
                   @RequestParam(required = false) Long roomTypeId,
                   @RequestParam(required = false) String stayType) {
        addBookingPageData(model, customerName, phone, checkInDate, roomTypeId, stayType);
        return "bookings/index :: bookingWorkspace";
    }

    private void addBookingPageData(Model model, String customerName, String phone, LocalDate checkInDate, Long roomTypeId, String stayType) {
        var roomTypeList = roomTypes.findAllByOrderByNameAsc();
        var availableRoomCounts = new HashMap<Long, Long>();
        var today = LocalDate.now();
        var tomorrow = today.plusDays(1);
        for (var roomType : roomTypeList) {
            availableRoomCounts.put(roomType.getId(), countAvailableRooms(roomType, today, tomorrow, null));
        }
        var normalizedCustomerName = customerName != null && !customerName.isBlank() ? customerName.trim() : "";
        var normalizedPhone = phone != null && !phone.isBlank() ? phone.trim() : "";
        model.addAttribute("bookings", bookings.searchBookings(
                normalizedCustomerName,
                normalizedPhone,
                checkInDate != null,
                checkInDate != null ? checkInDate : LocalDate.of(1900, 1, 1),
                roomTypeId != null,
                roomTypeId != null ? roomTypeId : 0L,
                stayType != null,
                stayType != null ? stayType : LookupCodes.DAILY
        ));
        model.addAttribute("roomTypes", roomTypeList);
        model.addAttribute("rooms", rooms.findAllByOrderByRoomNumber());
        model.addAttribute("availableRoomCounts", availableRoomCounts);
        model.addAttribute("searchCustomerName", customerName);
        model.addAttribute("searchPhone", phone);
        model.addAttribute("searchCheckInDate", checkInDate);
        model.addAttribute("searchRoomTypeId", roomTypeId);
        model.addAttribute("searchStayType", stayType);
        model.addAttribute("statuses", java.util.List.of("PENDING", "CHECKED_IN", "CANCELLED"));
        model.addAttribute("stayTypes", java.util.List.of("DAILY", "MONTHLY"));
        var booking = new Booking();
        booking.setDepositAmount(settings.defaultDeposit());
        booking.setNationality("ไทย");
        model.addAttribute("booking", booking);
        model.addAttribute("defaultDepositAmount", settings.defaultDeposit());
    }

    @PostMapping
    @Transactional
    Object save(@ModelAttribute Booking booking, @RequestParam Long roomTypeId,
                @RequestParam(required = false) Long roomId,
                @RequestHeader(value = "X-Requested-With", required = false) String requestedWith,
                RedirectAttributes redirect) {
        boolean isNew = booking.getId() == null;
        var roomType = roomTypes.findById(roomTypeId).orElseThrow();
        var selectedRoom = roomId != null ? rooms.findById(roomId).orElseThrow() : null;
        if (selectedRoom != null && (selectedRoom.getRoomType() == null || !selectedRoom.getRoomType().getId().equals(roomType.getId()))) {
            return bookingSaveError("ห้องที่เลือกไม่ตรงกับประเภทห้อง", requestedWith, redirect);
        }
        Booking savedBooking = isNew ? new Booking() : bookings.findById(booking.getId()).orElseThrow();
        var checkInDate = booking.getCheckInDate();
        var checkOutDate = booking.getCheckOutDate();
        var isMonthly = LookupCodes.MONTHLY.equals(booking.getStayType());
        if (isMonthly) {
            checkOutDate = null;
        } else if (checkInDate != null && (checkOutDate == null || checkInDate.isAfter(checkOutDate))) {
            checkOutDate = checkInDate.plusDays(1);
        }
        var availabilityCheckOutDate = isMonthly && checkInDate != null ? checkInDate.plusDays(1) : checkOutDate;
        if (checkInDate != null && availabilityCheckOutDate != null && countAvailableRooms(roomType, checkInDate, availabilityCheckOutDate, savedBooking.getId()) <= 0) {
            return bookingSaveError("ไม่มีห้องว่างสำหรับประเภทห้องและช่วงวันที่เลือก", requestedWith, redirect);
        }
        if (selectedRoom != null && checkInDate != null && availabilityCheckOutDate != null && countAvailableSelectedRoom(selectedRoom, checkInDate, availabilityCheckOutDate, savedBooking.getId()) <= 0) {
            return bookingSaveError("ห้องที่เลือกไม่ว่างในช่วงวันที่เลือก", requestedWith, redirect);
        }
        var previousRoom = savedBooking.getRoom();
        var roomChanged = previousRoom != null && (selectedRoom == null || !previousRoom.getId().equals(selectedRoom.getId()));
        if (!isNew && roomChanged && LookupCodes.RESERVED.equals(previousRoom.getStatus())) {
            previousRoom.setStatus(LookupCodes.AVAILABLE);
            rooms.save(previousRoom);
        }

        savedBooking.setCustomerName(booking.getCustomerName());
        savedBooking.setPhone(booking.getPhone());
        savedBooking.setIdCard(booking.getIdCard());
        savedBooking.setNationality(booking.getNationality());
        savedBooking.setCheckInDate(checkInDate);
        savedBooking.setCheckOutDate(checkOutDate);
        savedBooking.setStayType(booking.getStayType());
        savedBooking.setDepositAmount(booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO);
        savedBooking.setStatus(booking.getStatus());
        savedBooking.setNote(booking.getNote());
        savedBooking.setRoomType(roomType);
        savedBooking.setRoom(selectedRoom);
        if (savedBooking.getBookingDate() == null) {
            savedBooking.setBookingDate(LocalDate.now());
        }
        if (savedBooking.getBookingNumber() == null || savedBooking.getBookingNumber().isBlank()) {
            savedBooking.setBookingNumber(nextBookingNumber(savedBooking.getBookingDate()));
        }
        bookings.save(savedBooking);
        if (selectedRoom != null && LookupCodes.AVAILABLE.equals(selectedRoom.getStatus())) {
            selectedRoom.setStatus(LookupCodes.RESERVED);
            rooms.save(selectedRoom);
        }
        createOrUpdateBookingDepositReceipt(savedBooking);
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
        booking.setStatus(LookupCodes.CANCELLED);
        bookings.save(booking);
        var room = booking.getRoom();
        if (room != null && LookupCodes.RESERVED.equals(room.getStatus())) {
            room.setStatus(LookupCodes.AVAILABLE);
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

    private Object bookingSaveError(String message, String requestedWith, RedirectAttributes redirect) {
        if (isAjax(requestedWith)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "type", "warning",
                    "message", message
            ));
        }
        redirect.addFlashAttribute("error", message);
        redirect.addFlashAttribute("flashType", "warning");
        return "redirect:/bookings";
    }

    private String nextBookingNumber(LocalDate bookingDate) {
        var date = bookingDate != null ? bookingDate : LocalDate.now();
        var prefix = "B" + date.getYear();
        var maxBookingNumber = bookings.findMaxBookingNumberByPrefix(prefix);
        var nextSequence = 1;
        if (maxBookingNumber != null && maxBookingNumber.length() > prefix.length()) {
            nextSequence = Integer.parseInt(maxBookingNumber.substring(prefix.length())) + 1;
        }
        return "%s%06d".formatted(prefix, nextSequence);
    }

    private void createOrUpdateBookingDepositReceipt(Booking booking) {
        BigDecimal depositAmount = booking.getDepositAmount() == null ? BigDecimal.ZERO : booking.getDepositAmount();
        if (depositAmount.compareTo(BigDecimal.ZERO) <= 0 || booking.getBookingNumber() == null || booking.getBookingNumber().isBlank()) {
            return;
        }
        var payment = payments.findFirstByBookingIdOrderByIdAsc(booking.getId()).orElseGet(Payment::new);
        payment.setBooking(booking);
        payment.setRoom(booking.getRoom());
        payment.setGuest(null);
        payment.setAmount(depositAmount);
        payment.setFineAmount(BigDecimal.ZERO);
        payment.setPaymentDate(booking.getBookingDate() == null ? LocalDate.now() : booking.getBookingDate());
        payment.setPaymentMethod("เงินสด");
        payment.setStatus(LookupCodes.PAID);
        payment.setRemark(null);
        payment = payments.save(payment);
        recieptRecordService.recordBookingDeposit(payment);
        if (payment.getReciept() != null) {
            payment.getReciept().setAmount(payment.getTotalAmount());
        }
        payments.save(payment);
    }

    private long countAvailableRooms(com.hotel.model.RoomType roomType, LocalDate checkInDate, LocalDate checkOutDate, Long excludeId) {
        long availableByRoomStatus = rooms.countByRoomTypeAndStatusIn(roomType, List.of(LookupCodes.AVAILABLE, LookupCodes.RESERVED));
        if (checkInDate == null || checkOutDate == null) {
            return availableByRoomStatus;
        }
        long overlappingBookings = bookings.countOverlappingRoomTypeBookings(
                roomType,
                List.of(LookupCodes.PENDING, LookupCodes.CHECKED_IN),
                checkInDate,
                checkOutDate,
                excludeId
        );
        return Math.max(0, availableByRoomStatus - overlappingBookings);
    }

    private long countAvailableSelectedRoom(com.hotel.model.Room room, LocalDate checkInDate, LocalDate checkOutDate, Long excludeId) {
        if (!LookupCodes.AVAILABLE.equals(room.getStatus()) && !LookupCodes.RESERVED.equals(room.getStatus())) {
            return 0;
        }
        long overlappingBookings = bookings.countOverlappingRoomBookings(
                room,
                List.of(LookupCodes.PENDING, LookupCodes.CHECKED_IN),
                checkInDate,
                checkOutDate,
                excludeId
        );
        return overlappingBookings == 0 ? 1 : 0;
    }
}
