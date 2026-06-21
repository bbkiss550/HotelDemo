package com.hotel.controller;

import com.hotel.model.RoomType;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTypeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/room-types")
public class RoomTypeController {
    private final RoomTypeRepository roomTypes;
    private final RoomRepository rooms;

    public RoomTypeController(RoomTypeRepository roomTypes, RoomRepository rooms) {
        this.roomTypes = roomTypes;
        this.rooms = rooms;
    }

    @GetMapping
    String index(Model model) {
        model.addAttribute("roomTypes", roomTypes.findAllByOrderByNameAsc());
        model.addAttribute("roomType", new RoomType());
        return "room-types/index";
    }

    @PostMapping
    String save(@ModelAttribute RoomType roomType) {
        roomTypes.save(roomType);
        return "redirect:/room-types";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id) {
        var roomType = roomTypes.findById(id).orElseThrow();
        if (rooms.countByRoomType(roomType) == 0) {
            roomTypes.delete(roomType);
        }
        return "redirect:/room-types";
    }
}
