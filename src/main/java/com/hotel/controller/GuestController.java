package com.hotel.controller;

import com.hotel.model.Guest;
import com.hotel.model.RoomStatus;
import com.hotel.model.StayType;
import com.hotel.repository.FloorRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/guests")
public class GuestController {
    private final GuestRepository guests;
    private final RoomRepository rooms;
    private final FloorRepository floors;

    public GuestController(GuestRepository guests, RoomRepository rooms, FloorRepository floors) {
        this.guests = guests;
        this.rooms = rooms;
        this.floors = floors;
    }

    @GetMapping
    String index(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) Long floorId, Model model) {
        var floorList = floors.findAllByOrderBySortOrderAscNumberAscNameAsc();
        var selectedFloor = floorId == null
                ? floorList.stream().findFirst().orElse(null)
                : floors.findById(floorId).orElse(floorList.stream().findFirst().orElse(null));

        model.addAttribute("q", q);
        model.addAttribute("floors", floorList);
        model.addAttribute("selectedFloor", selectedFloor);
        var roomList = selectedFloor == null
                ? java.util.List.<com.hotel.model.Room>of()
                : q.isBlank()
                        ? rooms.findByFloorOrderByRoomNumber(selectedFloor)
                        : rooms.findByFloorAndRoomNumberContainingIgnoreCaseOrderByRoomNumber(selectedFloor, q);
        Map<Long, String> roomStayTypes = roomList.stream()
                .collect(Collectors.toMap(
                        com.hotel.model.Room::getId,
                        room -> guests.findTopByRoomOrderByCheckInDateDescIdDesc(room)
                                .map(guest -> guest.getStayType().name())
                                .orElse("")
                ));

        model.addAttribute("rooms", roomList);
        model.addAttribute("roomStayTypes", roomStayTypes);
        model.addAttribute("stayTypes", StayType.values());
        return "guests/index";
    }

    @GetMapping("/new")
    String create(Model model) {
        formData(model, new Guest());
        return "guests/form";
    }

    @GetMapping("/{id}/edit")
    String edit(@PathVariable Long id, Model model) {
        formData(model, guests.findById(id).orElseThrow());
        return "guests/form";
    }

    @PostMapping
    String save(@ModelAttribute Guest guest,
                @RequestParam Long roomId,
                @RequestParam(required = false) Long floorId) {
        var room = rooms.findById(roomId).orElseThrow();
        guest.setRoom(room);
        if (guest.getStayType() == StayType.DAILY) {
            guest.setAdvanceMonths(null);
            guest.setDeposit(BigDecimal.ZERO);
            guest.setInitialPayment(guest.getPrice() == null ? BigDecimal.ZERO : guest.getPrice());
        } else {
            BigDecimal price = guest.getPrice() == null ? BigDecimal.ZERO : guest.getPrice();
            BigDecimal deposit = guest.getDeposit() == null ? BigDecimal.ZERO : guest.getDeposit();
            int months = guest.getAdvanceMonths() == null ? 1 : guest.getAdvanceMonths();
            guest.setInitialPayment(price.multiply(BigDecimal.valueOf(months)).add(deposit));
        }
        if (guest.getCheckInDate() == null) {
            guest.setCheckInDate(LocalDate.now());
        }
        guests.save(guest);
        room.setStatus(RoomStatus.OCCUPIED);
        rooms.save(room);
        return floorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + floorId;
    }

    private void formData(Model model, Guest guest) {
        model.addAttribute("guest", guest);
        model.addAttribute("rooms", rooms.findAllByOrderByRoomNumber());
    }
}
