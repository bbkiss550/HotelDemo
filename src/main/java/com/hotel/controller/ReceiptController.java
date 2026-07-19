package com.hotel.controller;

import com.hotel.model.Payment;
import com.hotel.model.StayType;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.PaymentDetailRepository;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/receipts")
public class ReceiptController {
    private final PaymentRepository payments;
    private final PaymentDetailRepository paymentDetails;
    private final BookingRepository bookings;

    public ReceiptController(PaymentRepository payments, PaymentDetailRepository paymentDetails, BookingRepository bookings) {
        this.payments = payments;
        this.paymentDetails = paymentDetails;
        this.bookings = bookings;
    }

    @GetMapping("/{id}")
    String detail(@PathVariable Long id, Model model) {
        Payment payment = payments.findById(id).orElseThrow();
        model.addAttribute("payment", payment);
        model.addAttribute("receiptNo", payment.getReciept() != null ? payment.getReciept().getRecieptNo() : null);
        model.addAttribute("receiptTypeName", payment.getReciept() != null && payment.getReciept().getType() != null ? payment.getReciept().getType().getName() : "-");
        model.addAttribute("billNumber", monthlyRentBillNumber(payment));
        model.addAttribute("payerName", paymentPayerName(payment));
        model.addAttribute("receiptItems", receiptItems(payment));
        return "receipts/detail";
    }

    private String monthlyRentBillNumber(Payment payment) {
        if (payment != null && payment.getMonthlyRentBill() != null) {
            return payment.getMonthlyRentBill().getDisplayBillNumber();
        }
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

    private String paymentPayerName(Payment payment) {
        if (payment != null && payment.getBooking() != null
                && payment.getBooking().getCustomerName() != null
                && !payment.getBooking().getCustomerName().isBlank()) {
            return payment.getBooking().getCustomerName();
        }
        String bookingNumber = bookingNumberFromRemark(payment == null ? null : payment.getRemark());
        if (bookingNumber != null) {
            return bookings.findByBookingNumber(bookingNumber)
                    .map(com.hotel.model.Booking::getCustomerName)
                    .filter(name -> name != null && !name.isBlank())
                    .orElse("-");
        }
        return payment != null && payment.getGuest() != null && payment.getGuest().getFullName() != null
                ? payment.getGuest().getFullName()
                : "-";
    }

    private String bookingNumberFromRemark(String remark) {
        if (remark == null || remark.isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("#(B\\d{10})").matcher(remark);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<ReceiptLine> receiptItems(Payment payment) {
        if (payment == null || payment.getId() == null) {
            return List.of();
        }
        return paymentDetails.findByPaymentIdOrderBySortOrderAscIdAsc(payment.getId()).stream()
                .map(detail -> new ReceiptLine(
                        detail.getItem().getName(),
                        detail.getQuantity(),
                        detail.getUnitPrice(),
                        detail.getAmount()))
                .toList();
    }

    private List<ReceiptLine> legacyReceiptItems(Payment payment) {
        if (payment == null || payment.getGuest() == null || payment.getGuest().getStayType() != StayType.DAILY) {
            return List.of(new ReceiptLine(payment != null && payment.getReciept() != null && payment.getReciept().getType() != null ? payment.getReciept().getType().getName() : "-", BigDecimal.ONE, payment == null ? BigDecimal.ZERO : payment.getAmount(), payment == null ? BigDecimal.ZERO : payment.getAmount()));
        }
        var guest = payment.getGuest();
        BigDecimal price = guest.getPrice() == null ? BigDecimal.ZERO : guest.getPrice();
        BigDecimal deposit = guest.getDeposit() == null ? BigDecimal.ZERO : guest.getDeposit();
        long days = 1;
        if (guest.getCheckInDate() != null && guest.getCheckOutDate() != null) {
            days = Math.max(1, ChronoUnit.DAYS.between(guest.getCheckInDate(), guest.getCheckOutDate()));
        }
        BigDecimal roomAmount = price.multiply(BigDecimal.valueOf(days));
        BigDecimal paidAmount = payment.getAmount() == null ? BigDecimal.ZERO : payment.getAmount();
        BigDecimal bookingDeposit = roomAmount.add(deposit).subtract(paidAmount).max(BigDecimal.ZERO);
        List<ReceiptLine> items = new ArrayList<>();
        if (roomAmount.compareTo(BigDecimal.ZERO) > 0) {
            items.add(new ReceiptLine("ค่าห้องรายวัน", BigDecimal.valueOf(days), price, roomAmount));
        }
        if (deposit.compareTo(BigDecimal.ZERO) > 0) {
            items.add(new ReceiptLine("ค่าประกัน", BigDecimal.ONE, deposit, deposit));
        }
        if (bookingDeposit.compareTo(BigDecimal.ZERO) > 0) {
            items.add(new ReceiptLine("หักมัดจำจอง", BigDecimal.ONE, bookingDeposit.negate(), bookingDeposit.negate()));
        }
        if (items.isEmpty()) {
            items.add(new ReceiptLine("ค่าบริการของลูกค้ารายวัน", BigDecimal.ONE, paidAmount, paidAmount));
        }
        return items;
    }

    public record ReceiptLine(String label, BigDecimal units, BigDecimal unitPrice, BigDecimal amount) {
    }
}
