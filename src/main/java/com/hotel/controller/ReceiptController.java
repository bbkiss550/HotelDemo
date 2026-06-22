package com.hotel.controller;

import com.hotel.repository.ReceiptRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/receipts")
public class ReceiptController {
    private final ReceiptRepository receipts;

    public ReceiptController(ReceiptRepository receipts) {
        this.receipts = receipts;
    }

    @GetMapping("/{id}")
    String detail(@PathVariable Long id, Model model) {
        model.addAttribute("receipt", receipts.findById(id).orElseThrow());
        return "receipts/detail";
    }
}
