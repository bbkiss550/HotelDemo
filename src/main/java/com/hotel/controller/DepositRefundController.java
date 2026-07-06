package com.hotel.controller;

import com.hotel.model.DepositRefund;
import com.hotel.repository.DepositRefundRepository;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/finance/deposit-refunds")
public class DepositRefundController {
    private final DepositRefundRepository refunds;

    public DepositRefundController(DepositRefundRepository refunds) {
        this.refunds = refunds;
    }

    @GetMapping
    String index(@RequestParam(defaultValue = "") String q, Model model) {
        var refundList = refunds.findAllByOrderByRefundDateDescIdDesc().stream()
                .filter(refund -> matches(refund, q))
                .toList();
        model.addAttribute("refunds", refundList);
        model.addAttribute("q", q);
        return "finance/deposit-refunds/index";
    }

    private boolean matches(DepositRefund refund, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String term = q.toLowerCase(Locale.ROOT);
        return contains(refund.getRefundNo(), term)
                || contains(refund.getGuest() == null ? null : refund.getGuest().getFullName(), term)
                || contains(refund.getRoom() == null ? null : refund.getRoom().getRoomNumber(), term);
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }
}
