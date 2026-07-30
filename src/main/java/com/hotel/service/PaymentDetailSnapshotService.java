package com.hotel.service;

import com.hotel.model.LookupCodes;

import com.hotel.model.MonthlyRentBill;
import com.hotel.model.Payment;
import com.hotel.model.PaymentDetail;
import com.hotel.model.PaymentItem;
import com.hotel.model.RecieptType;
import com.hotel.repository.PaymentDetailRepository;
import com.hotel.repository.PaymentItemRepository;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentDetailSnapshotService {
    private final PaymentDetailRepository details;
    private final PaymentItemRepository items;

    public PaymentDetailSnapshotService(PaymentDetailRepository details, PaymentItemRepository items) {
        this.details = details;
        this.items = items;
    }

    @Transactional
    public void snapshot(Payment payment) {
        if (payment == null || payment.getId() == null) return;
        details.deleteByPaymentId(payment.getId());
        List<Line> lines = lines(payment);
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            PaymentDetail detail = new PaymentDetail();
            detail.setPayment(payment);
            detail.setItem(item(line.name, line.englishName));
            detail.setQuantity(line.quantity);
            detail.setUnitPrice(line.unitPrice);
            detail.setAmount(line.amount);
            detail.setSortOrder(i + 1);
            details.save(detail);
        }
    }

    private List<Line> lines(Payment payment) {
        if (payment.getMonthlyRentBill() != null) return monthlyLines(payment);
        if (isPenalty(payment)) return penaltyLines(payment);
        if (payment.getBooking() != null) return List.of(line("มัดจำจองห้อง", "Booking deposit", 1, payment.getAmount()));
        if (payment.getGuest() != null && LookupCodes.MONTHLY.equals(payment.getGuest().getStayType())) return monthlyOpeningLines(payment);
        if (payment.getGuest() != null && LookupCodes.DAILY.equals(payment.getGuest().getStayType())) return dailyLines(payment);
        String name = payment.getReciept() != null && payment.getReciept().getType() != null
                ? payment.getReciept().getType().getName() : "ค่าบริการของลูกค้ารายวัน";
        return List.of(line(name, "Service charge", 1, payment.getAmount()));
    }

    private List<Line> monthlyLines(Payment payment) {
        MonthlyRentBill bill = payment.getMonthlyRentBill();
        List<Line> lines = new ArrayList<>();
        add(lines, "ค่าเช่ารายเดือน", "Monthly rent", 1, money(bill.getRentAmount()));
        add(lines, "ค่าน้ำ", "Water charge", money(bill.getWaterUnit()), money(bill.getWaterRate()));
        add(lines, "ค่าไฟ", "Electricity charge", money(bill.getElectricUnit()), money(bill.getElectricRate()));
        add(lines, "ค่าอื่น ๆ", "Other charge", 1, money(bill.getOtherAmount()));
        add(lines, "ส่วนลด", "Discount", 1, money(bill.getDiscountAmount()).negate());
        add(lines, "หักเงินล่วงหน้า", "Advance payment deduction", 1, money(bill.getAdvanceAppliedAmount()).negate());
        add(lines, "ค่าปรับ", "Penalty", 1, money(payment.getFineAmount()));
        return lines.isEmpty() ? List.of(line("ค่าเช่ารายเดือน", "Monthly rent", 1, payment.getAmount())) : lines;
    }

    private List<Line> dailyLines(Payment payment) {
        var guest = payment.getGuest();
        BigDecimal price = money(guest.getPrice());
        long days = guest.getCheckInDate() != null && guest.getCheckOutDate() != null
                ? Math.max(1, ChronoUnit.DAYS.between(guest.getCheckInDate(), guest.getCheckOutDate())) : 1;
        BigDecimal roomAmount = price.multiply(BigDecimal.valueOf(days));
        BigDecimal deposit = money(guest.getDeposit());
        BigDecimal bookingDeposit = roomAmount.add(deposit).subtract(money(payment.getAmount())).max(BigDecimal.ZERO);
        List<Line> lines = new ArrayList<>();
        add(lines, "ค่าห้องรายวัน", "Daily room charge", days, price);
        add(lines, "ค่าประกัน", "Deposit", 1, deposit);
        add(lines, "หักมัดจำจอง", "Booking deposit deduction", 1, bookingDeposit.negate());
        return lines.isEmpty() ? List.of(line("ค่าบริการของลูกค้ารายวัน", "Daily guest service", 1, payment.getAmount())) : lines;
    }

    private List<Line> monthlyOpeningLines(Payment payment) {
        var guest = payment.getGuest();
        int months = Math.max(guest.getAdvanceMonths() == null ? 1 : guest.getAdvanceMonths(), 1);
        List<Line> lines = new ArrayList<>();
        add(lines, "ชำระค่าห้องล่วงหน้า", "Advance room payment",
                months, money(guest.getPrice()));
        add(lines, "ค่าประกัน", "Deposit", 1, money(guest.getDeposit()));
        return lines.isEmpty() ? List.of(line("ชำระค่าห้องล่วงหน้า", "Advance room payment", 1, payment.getAmount())) : lines;
    }

    private List<Line> penaltyLines(Payment payment) {
        List<Line> lines = new ArrayList<>();
        if (payment.getDepositRefund() != null) {
            payment.getDepositRefund().getItems().forEach(item -> add(lines,
                    item.getItemName() != null && item.getItemName().contains("ค่าเสียหาย") ? "ค่าเสียหาย" : "ค่าปรับเช็กเอาต์ล่าช้า",
                    item.getItemNameEn() == null || item.getItemNameEn().isBlank() ? "Late check-out penalty" : item.getItemNameEn(),
                    1, money(item.getItemAmount())));
            BigDecimal deducted = money(payment.getDepositRefund().getDepositAmount()).min(money(payment.getDepositRefund().getTotalDeductAmount()));
            add(lines, "หักค่าประกัน", "Deposit deduction", 1, deducted.negate());
        }
        return lines.isEmpty() ? List.of(line("ค่าปรับ", "Penalty", 1, payment.getAmount())) : lines;
    }

    private boolean isPenalty(Payment payment) {
        return payment.getReciept() != null && payment.getReciept().getType() != null
                && RecieptType.PENALTY == payment.getReciept().getType().getId();
    }

    private void add(List<Line> lines, String name, String englishName, long quantity, BigDecimal unitPrice) {
        add(lines, name, englishName, BigDecimal.valueOf(quantity), unitPrice);
    }
    private void add(List<Line> lines, String name, String englishName, BigDecimal quantity, BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) == 0) return;
        lines.add(line(name, englishName, quantity, unitPrice));
    }
    private Line line(String name, String englishName, long quantity, BigDecimal unitPrice) {
        return line(name, englishName, BigDecimal.valueOf(quantity), unitPrice);
    }
    private Line line(String name, String englishName, BigDecimal quantity, BigDecimal unitPrice) {
        BigDecimal amount = money(quantity).multiply(money(unitPrice));
        return new Line(name, englishName, money(quantity), money(unitPrice), amount);
    }
    private PaymentItem item(String name, String englishName) {
        return items.findByName(name).orElseGet(() -> {
            PaymentItem item = new PaymentItem();
            item.setName(name);
            item.setNameEn(englishName);
            return items.save(item);
        });
    }
    private BigDecimal money(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private record Line(String name, String englishName, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) { }
}
