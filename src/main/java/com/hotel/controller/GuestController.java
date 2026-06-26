package com.hotel.controller;

import com.hotel.model.Guest;
import com.hotel.model.RoomStatus;
import com.hotel.model.StayType;
import com.hotel.repository.FloorRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.AuditService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/guests")
public class GuestController {
    private final GuestRepository guests;
    private final RoomRepository rooms;
    private final FloorRepository floors;
    private final PaymentRepository payments;
    private final AuditService audit;

    public GuestController(GuestRepository guests, RoomRepository rooms, FloorRepository floors, PaymentRepository payments, AuditService audit) {
        this.guests = guests;
        this.rooms = rooms;
        this.floors = floors;
        this.payments = payments;
        this.audit = audit;
    }

    @GetMapping
    String index(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) Long floorId, Model model) {
        var floorList = floors.findAllByOrderBySortOrderAscNumberAscNameAsc();
        var selectedFloor = floorId == null
                ? floorList.stream().findFirst().orElse(null)
                : floors.findById(floorId).orElse(floorList.stream().findFirst().orElse(null));

        var roomList = q.isBlank()
                ? rooms.findAllByOrderByRoomNumber()
                : rooms.findByRoomNumberContainingIgnoreCaseOrderByRoomNumber(q);
        Map<Long, Guest> activeGuests = new HashMap<>();
        for (var room : roomList) {
            guests.findTopByRoomAndActiveTrueOrderByCheckInDateDescIdDesc(room)
                    .ifPresent(guest -> activeGuests.put(room.getId(), guest));
        }

        model.addAttribute("q", q);
        model.addAttribute("floors", floorList);
        model.addAttribute("selectedFloor", selectedFloor);
        model.addAttribute("rooms", roomList);
        model.addAttribute("activeGuests", activeGuests);
        model.addAttribute("stayTypes", StayType.values());
        return "guests/index";
    }

    @PostMapping
    String save(@ModelAttribute Guest guest, @RequestParam Long roomId, @RequestParam(required = false) Long floorId, RedirectAttributes redirect) {
        var room = rooms.findById(roomId).orElseThrow();
        guest.setRoom(room);
        guest.setActive(true);
        calculateGuestAmounts(guest);
        if (guest.getCheckInDate() == null) {
            guest.setCheckInDate(LocalDate.now());
        }
        guests.save(guest);
        room.setStatus(guest.getStayType() == StayType.MONTHLY ? RoomStatus.MONTHLY_OCCUPIED : RoomStatus.DAILY_OCCUPIED);
        rooms.save(room);
        audit.record("CHECK_IN", "Room " + room.getRoomNumber() + " guest " + guest.getFullName());
        redirect.addFlashAttribute("message", "บันทึกเข้าพักห้อง " + room.getRoomNumber() + " เรียบร้อย");
        redirect.addFlashAttribute("flashType", "success");
        return floorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + floorId;
    }

    @PostMapping("/{id}/checkout")
    String checkout(@PathVariable Long id, @RequestParam(required = false) Long floorId, RedirectAttributes redirect) {
        var guest = guests.findById(id).orElseThrow();
        guest.setActive(false);
        if (guest.getCheckOutDate() == null) {
            guest.setCheckOutDate(LocalDate.now());
        }
        guests.save(guest);
        var room = guest.getRoom();
        room.setStatus(RoomStatus.AVAILABLE);
        rooms.save(room);
        audit.record("CHECK_OUT", "Room " + room.getRoomNumber() + " guest " + guest.getFullName());
        redirect.addFlashAttribute("message", "เช็กเอาต์ห้อง " + room.getRoomNumber() + " เรียบร้อย");
        redirect.addFlashAttribute("flashType", "delete");
        return floorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + floorId;
    }

    @GetMapping("/{id}")
    String detail(@PathVariable Long id, Model model) {
        var guest = guests.findById(id).orElseThrow();
        model.addAttribute("guest", guest);
        model.addAttribute("payments", payments.findByGuestIdOrderByPaymentDateDescIdDesc(id));
        return "guests/detail";
    }

    private void calculateGuestAmounts(Guest guest) {
        if (guest.getStayType() == StayType.DAILY) {
            guest.setAdvanceMonths(null);
            BigDecimal price = guest.getPrice() == null ? BigDecimal.ZERO : guest.getPrice();
            BigDecimal deposit = guest.getDeposit() == null ? BigDecimal.ZERO : guest.getDeposit();
            long days = 1;
            if (guest.getCheckInDate() != null && guest.getCheckOutDate() != null) {
                days = Math.max(1, ChronoUnit.DAYS.between(guest.getCheckInDate(), guest.getCheckOutDate()));
            }
            guest.setInitialPayment(price.multiply(BigDecimal.valueOf(days)).add(deposit));
        } else {
            BigDecimal price = guest.getPrice() == null ? BigDecimal.ZERO : guest.getPrice();
            BigDecimal deposit = guest.getDeposit() == null ? BigDecimal.ZERO : guest.getDeposit();
            int months = guest.getAdvanceMonths() == null ? 1 : guest.getAdvanceMonths();
            guest.setInitialPayment(price.multiply(BigDecimal.valueOf(months)).add(deposit));
        }
        if (guest.getTotalPaid() == null) {
            guest.setTotalPaid(BigDecimal.ZERO);
        }
    }
}
