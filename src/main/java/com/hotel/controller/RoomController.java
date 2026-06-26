package com.hotel.controller;

import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.repository.FloorRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/rooms")
public class RoomController {
    private final RoomRepository rooms;
    private final FloorRepository floors;
    private final RoomTypeRepository roomTypes;
    private final GuestRepository guests;

    public RoomController(RoomRepository rooms, FloorRepository floors, RoomTypeRepository roomTypes, GuestRepository guests) {
        this.rooms = rooms;
        this.floors = floors;
        this.roomTypes = roomTypes;
        this.guests = guests;
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

        model.addAttribute("q", q);
        model.addAttribute("floors", floorList);
        model.addAttribute("roomTypes", roomTypes.findAllByOrderByNameAsc());
        model.addAttribute("selectedFloor", selectedFloor);
        Map<Long, String> roomStayTypes = roomList.stream()
                .collect(Collectors.toMap(
                        com.hotel.model.Room::getId,
                        room -> guests.findTopByRoomAndActiveTrueOrderByCheckInDateDescIdDesc(room)
                                .map(guest -> guest.getStayType().name())
                                .orElse("")
                ));

        model.addAttribute("rooms", roomList);
        model.addAttribute("roomStayTypes", roomStayTypes);
        model.addAttribute("statuses", RoomStatus.values());
        return "rooms/index";
    }

    @GetMapping("/new")
    String create(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("floors", floors.findAllByOrderBySortOrderAscNumberAscNameAsc());
        model.addAttribute("roomTypes", roomTypes.findAllByOrderByNameAsc());
        model.addAttribute("statuses", RoomStatus.values());
        return "rooms/form";
    }

    @GetMapping("/{id}/edit")
    String edit(@PathVariable Long id, Model model) {
        model.addAttribute("room", rooms.findById(id).orElseThrow());
        model.addAttribute("floors", floors.findAllByOrderBySortOrderAscNumberAscNameAsc());
        model.addAttribute("roomTypes", roomTypes.findAllByOrderByNameAsc());
        model.addAttribute("statuses", RoomStatus.values());
        return "rooms/form";
    }

    @PostMapping
    String save(@ModelAttribute Room room, @RequestParam Long floorId, @RequestParam Long roomTypeId, RedirectAttributes redirect) {
        Room savedRoom = room.getId() == null ? room : rooms.findById(room.getId()).orElseThrow();
        boolean isNew = savedRoom.getId() == null;
        savedRoom.setRoomNumber(room.getRoomNumber());
        savedRoom.setFloor(floors.findById(floorId).orElseThrow());
        var roomType = roomTypes.findById(roomTypeId).orElseThrow();
        savedRoom.setRoomType(roomType);
        savedRoom.setNightlyPrice(roomType.getNightlyPrice());
        savedRoom.setMonthlyPrice(roomType.getMonthlyPrice());
        if (savedRoom.getStatus() == null) {
            savedRoom.setStatus(RoomStatus.AVAILABLE);
        }
        rooms.save(savedRoom);
        redirect.addFlashAttribute("message", (isNew ? "บันทึกห้อง " : "แก้ไขห้อง ") + savedRoom.getRoomNumber() + " เรียบร้อย");
        redirect.addFlashAttribute("flashType", isNew ? "success" : "edit");
        return "redirect:/rooms?floorId=" + floorId;
    }

    @PostMapping("/floors")
    String saveFloor(@RequestParam String name, @RequestParam(required = false) Integer number, RedirectAttributes redirect) {
        var floor = number == null ? new com.hotel.model.Floor() : floors.findByNumber(number).orElseGet(com.hotel.model.Floor::new);
        floor.setName(name);
        floor.setNumber(number);
        if (floor.getSortOrder() == null || floor.getSortOrder() == 0) {
            floor.setSortOrder((floors.findAllByOrderBySortOrderAscNumberAscNameAsc().size() + 1) * 10);
        }
        floors.save(floor);
        redirect.addFlashAttribute("message", "บันทึกชั้น " + floor.getName() + " เรียบร้อย");
        redirect.addFlashAttribute("flashType", "success");
        return "redirect:/rooms?floorId=" + floor.getId();
    }

    @PostMapping("/floors/{id}")
    String updateFloor(@PathVariable Long id, @RequestParam String name, @RequestParam(required = false) Integer number, RedirectAttributes redirect) {
        var floor = floors.findById(id).orElseThrow();
        floor.setName(name);
        floor.setNumber(number);
        floors.save(floor);
        redirect.addFlashAttribute("message", "แก้ไขชั้น " + floor.getName() + " เรียบร้อย");
        redirect.addFlashAttribute("flashType", "edit");
        return "redirect:/rooms?floorId=" + floor.getId();
    }

    @PostMapping("/floors/{id}/delete")
    String deleteFloor(@PathVariable Long id, RedirectAttributes redirect) {
        var floor = floors.findById(id).orElseThrow();
        if (rooms.countByFloor(floor) == 0) {
            String floorName = floor.getName();
            floors.delete(floor);
            redirect.addFlashAttribute("message", "ลบชั้น " + floorName + " เรียบร้อย");
            redirect.addFlashAttribute("flashType", "delete");
        } else {
            redirect.addFlashAttribute("error", "ลบชั้นไม่ได้ เพราะยังมีห้องอยู่ในชั้นนี้");
            redirect.addFlashAttribute("flashType", "warning");
        }
        return "redirect:/rooms";
    }

    @PostMapping("/floors/reorder")
    @ResponseBody
    String reorderFloors(@RequestParam List<Long> floorIds) {
        for (int i = 0; i < floorIds.size(); i++) {
            var floor = floors.findById(floorIds.get(i)).orElseThrow();
            floor.setSortOrder((i + 1) * 10);
            floors.save(floor);
        }
        return "ok";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id, RedirectAttributes redirect) {
        String roomNumber = rooms.findById(id).map(Room::getRoomNumber).orElse("");
        rooms.deleteById(id);
        redirect.addFlashAttribute("message", "ลบห้อง " + roomNumber + " เรียบร้อย");
        redirect.addFlashAttribute("flashType", "delete");
        return "redirect:/rooms";
    }
}
