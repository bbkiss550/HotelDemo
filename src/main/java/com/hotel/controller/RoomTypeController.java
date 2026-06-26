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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    String save(@ModelAttribute RoomType roomType, RedirectAttributes redirect) {
        boolean isNew = roomType.getId() == null;
        roomTypes.save(roomType);
        redirect.addFlashAttribute("message", (isNew ? "บันทึกประเภทห้อง " : "แก้ไขประเภทห้อง ") + roomType.getName() + " เรียบร้อย");
        redirect.addFlashAttribute("flashType", isNew ? "success" : "edit");
        return "redirect:/room-types";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id, RedirectAttributes redirect) {
        var roomType = roomTypes.findById(id).orElseThrow();
        if (rooms.countByRoomType(roomType) == 0) {
            String roomTypeName = roomType.getName();
            roomTypes.delete(roomType);
            redirect.addFlashAttribute("message", "ลบประเภทห้อง " + roomTypeName + " เรียบร้อย");
            redirect.addFlashAttribute("flashType", "delete");
        } else {
            redirect.addFlashAttribute("error", "ลบประเภทห้องไม่ได้ เพราะยังมีห้องใช้งานอยู่");
            redirect.addFlashAttribute("flashType", "warning");
        }
        return "redirect:/room-types";
    }
}
