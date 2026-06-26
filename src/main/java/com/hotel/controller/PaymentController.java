package com.hotel.controller;

import com.hotel.model.Payment;
import com.hotel.model.PaymentStatus;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.ReceiptRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.AuditService;
import com.hotel.service.ReceiptService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentRepository payments;
    private final RoomRepository rooms;
    private final GuestRepository guests;
    private final ReceiptRepository receipts;
    private final ReceiptService receiptService;
    private final AuditService audit;

    public PaymentController(PaymentRepository payments, RoomRepository rooms, GuestRepository guests, ReceiptRepository receipts, ReceiptService receiptService, AuditService audit) {
        this.payments = payments;
        this.rooms = rooms;
        this.guests = guests;
        this.receipts = receipts;
        this.receiptService = receiptService;
        this.audit = audit;
    }

    @GetMapping
    String index(Model model) {
        var paymentList = payments.findAllByOrderByPaymentDateDescIdDesc();
        model.addAttribute("payments", paymentList);
        java.util.Map<Long, com.hotel.model.Receipt> receiptsByPaymentId = new java.util.HashMap<>();
        for (Payment payment : paymentList) {
            receipts.findByPaymentId(payment.getId()).ifPresent(receipt -> receiptsByPaymentId.put(payment.getId(), receipt));
        }
        model.addAttribute("receiptsByPaymentId", receiptsByPaymentId);
        return "payments/index";
    }

    @GetMapping("/new")
    String create(@RequestParam(required = false) Long guestId, Model model) {
        Payment payment = new Payment();
        payment.setPaymentDate(LocalDate.now());
        if (guestId != null) {
            guests.findById(guestId).ifPresent(guest -> {
                payment.setGuest(guest);
                payment.setRoom(guest.getRoom());
                payment.setAmount(guest.getInitialPayment());
                payment.setStatus(PaymentStatus.PAID);
            });
        }
        formData(model, payment);
        return "payments/form";
    }

    @GetMapping("/{id}/edit")
    String edit(@PathVariable Long id, Model model) {
        formData(model, payments.findById(id).orElseThrow());
        return "payments/form";
    }

    @PostMapping
    String save(@ModelAttribute Payment payment, @RequestParam(required = false) Long guestId, @RequestParam Long roomId, RedirectAttributes redirect) {
        boolean isNew = payment.getId() == null;
        payment.setRoom(rooms.findById(roomId).orElseThrow());
        if (guestId != null) {
            payment.setGuest(guests.findById(guestId).orElse(null));
        }
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
        }
        payment = payments.save(payment);
        if (payment.getGuest() != null && payment.getStatus() == PaymentStatus.PAID) {
            var guest = payment.getGuest();
            BigDecimal totalPaid = guest.getTotalPaid() == null ? BigDecimal.ZERO : guest.getTotalPaid();
            guest.setTotalPaid(totalPaid.add(payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount()));
            guests.save(guest);
            receiptService.createForPaidPayment(payment);
        }
        audit.record("PAYMENT", "Room " + payment.getRoom().getRoomNumber() + " amount " + payment.getAmount());
        redirect.addFlashAttribute("message", (isNew ? "บันทึกการชำระเงิน" : "แก้ไขการชำระเงิน") + "เรียบร้อย");
        redirect.addFlashAttribute("flashType", isNew ? "success" : "edit");
        return "redirect:/payments";
    }

    private void formData(Model model, Payment payment) {
        model.addAttribute("payment", payment);
        model.addAttribute("rooms", rooms.findAllByOrderByRoomNumber());
        model.addAttribute("guests", guests.findByActiveTrueOrderByCheckInDateDescIdDesc());
        model.addAttribute("statuses", PaymentStatus.values());
    }
}
