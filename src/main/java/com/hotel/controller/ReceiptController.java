package com.hotel.controller;

import com.hotel.model.Payment;
import com.hotel.repository.PaymentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/receipts")
public class ReceiptController {
    private final PaymentRepository payments;

    public ReceiptController(PaymentRepository payments) {
        this.payments = payments;
    }

    @GetMapping("/{id}")
    String detail(@PathVariable Long id, Model model) {
        Payment payment = payments.findById(id).orElseThrow();
        model.addAttribute("payment", payment);
        model.addAttribute("receiptNo", payment.getReciept() != null ? payment.getReciept().getRecieptNo() : null);
        model.addAttribute("receiptTypeName", payment.getReciept() != null && payment.getReciept().getType() != null ? payment.getReciept().getType().getName() : "-");
        model.addAttribute("billNumber", monthlyRentBillNumber(payment));
        return "receipts/detail";
    }

    private String monthlyRentBillNumber(Payment payment) {
        String remark = payment == null ? null : payment.getRemark();
        if (remark == null || remark.isBlank()) {
            return "-";
        }
        int marker = remark.indexOf('#');
        if (marker < 0 || marker + 1 >= remark.length()) {
            return "-";
        }
        String value = remark.substring(marker + 1).split("\\s|-", 2)[0].trim();
        return value.isBlank() ? "-" : value;
    }
}
