package com.hotel.controller;

import com.hotel.model.Payment;
import com.hotel.model.PaymentStatus;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.RoomRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentRepository payments;
    private final RoomRepository rooms;

    public PaymentController(PaymentRepository payments, RoomRepository rooms) {
        this.payments = payments;
        this.rooms = rooms;
    }

    @GetMapping
    String index(Model model) {
        model.addAttribute("payments", payments.findAll());
        return "payments/index";
    }

    @GetMapping("/new")
    String create(Model model) {
        formData(model, new Payment());
        return "payments/form";
    }

    @GetMapping("/{id}/edit")
    String edit(@PathVariable Long id, Model model) {
        formData(model, payments.findById(id).orElseThrow());
        return "payments/form";
    }

    @PostMapping
    String save(@ModelAttribute Payment payment) {
        payments.save(payment);
        return "redirect:/payments";
    }

    private void formData(Model model, Payment payment) {
        model.addAttribute("payment", payment);
        model.addAttribute("rooms", rooms.findAllByOrderByRoomNumber());
        model.addAttribute("statuses", PaymentStatus.values());
    }
}
