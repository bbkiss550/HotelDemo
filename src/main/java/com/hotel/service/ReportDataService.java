package com.hotel.service;

import com.hotel.model.Payment;
import com.hotel.model.PaymentStatus;
import com.hotel.model.RoomStatus;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.DepositRefundRepository;
import com.hotel.repository.MonthlyRentBillRepository;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReportDataService {
    private final RoomRepository rooms;
    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final MonthlyRentBillRepository monthlyBills;
    private final DepositRefundRepository depositRefunds;

    public ReportDataService(RoomRepository rooms,
                             PaymentRepository payments,
                             BookingRepository bookings,
                             MonthlyRentBillRepository monthlyBills,
                             DepositRefundRepository depositRefunds) {
        this.rooms = rooms;
        this.payments = payments;
        this.bookings = bookings;
        this.monthlyBills = monthlyBills;
        this.depositRefunds = depositRefunds;
    }

    public DateRange range(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        LocalDate reportStart = startDate == null ? today.withDayOfMonth(1) : startDate;
        LocalDate reportEnd = endDate == null ? today : endDate;
        if (reportStart.isAfter(reportEnd)) {
            reportEnd = reportStart;
        }
        return new DateRange(reportStart, reportEnd);
    }

    public RevenueReport revenue(DateRange range) {
        List<Payment> paymentList = payments.findByCreatedAtBetweenOrderByReceiptNoAsc(range.startDateTime(), range.endExclusiveDateTime());
        BigDecimal totalRevenue = paymentList.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .map(Payment::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fineRevenue = paymentList.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .map(Payment::getFineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RevenueReport(
                paymentList,
                paymentPayerNames(paymentList),
                paymentReceiptNumbers(paymentList),
                totalRevenue,
                fineRevenue,
                revenueByType(paymentList),
                revenueByMethod(paymentList)
        );
    }

    public MonthlyBillReport monthlyBills(DateRange range) {
        var billList = monthlyBills.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(range.startDateTime(), range.endExclusiveDateTime());
        BigDecimal outstandingAmount = monthlyBills.findAll().stream()
                .map(bill -> bill.getRemainingAmount() == null ? BigDecimal.ZERO : bill.getRemainingAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MonthlyBillReport(billList, outstandingAmount);
    }

    public RoomReport rooms() {
        return new RoomReport(
                rooms.count(),
                rooms.countByStatus(RoomStatus.AVAILABLE),
                rooms.countByStatus(RoomStatus.DAILY_OCCUPIED) + rooms.countByStatus(RoomStatus.MONTHLY_OCCUPIED),
                roomStatusCounts()
        );
    }

    public BookingReport bookings(DateRange range) {
        return new BookingReport(bookings.findByBookingDateBetweenOrderByBookingNumberAscIdAsc(range.start(), range.end()));
    }

    public DepositRefundReport depositRefunds(DateRange range) {
        var refundList = depositRefunds.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDescIdDesc(range.startDateTime(), range.endExclusiveDateTime());
        BigDecimal refundAmount = refundList.stream()
                .map(refund -> refund.getRefundAmount() == null ? BigDecimal.ZERO : refund.getRefundAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DepositRefundReport(refundList, refundAmount);
    }

    private Map<String, BigDecimal> revenueByType(List<Payment> paymentList) {
        return paymentList.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .collect(Collectors.groupingBy(
                        payment -> payment.getReciept() != null && payment.getReciept().getType() != null ? payment.getReciept().getType().getName() : "ไม่ระบุ",
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getTotalAmount, BigDecimal::add)
                ));
    }

    private Map<String, BigDecimal> revenueByMethod(List<Payment> paymentList) {
        return paymentList.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .collect(Collectors.groupingBy(
                        payment -> payment.getPaymentMethod() == null || payment.getPaymentMethod().isBlank() ? "ไม่ระบุ" : payment.getPaymentMethod(),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getTotalAmount, BigDecimal::add)
                ));
    }

    private Map<String, Long> roomStatusCounts() {
        Map<String, Long> values = new LinkedHashMap<>();
        for (RoomStatus status : RoomStatus.values()) {
            values.put(status.getLabel(), rooms.countByStatus(status));
        }
        return values;
    }

    private Map<Long, String> paymentPayerNames(List<Payment> paymentList) {
        return paymentList.stream()
                .collect(Collectors.toMap(Payment::getId, this::paymentPayerName, (left, right) -> left));
    }

    private Map<Long, String> paymentReceiptNumbers(List<Payment> paymentList) {
        return paymentList.stream()
                .map(payment -> new AbstractMap.SimpleEntry<>(payment.getId(), payment.getReciept() == null
                        ? null
                        : payment.getReciept().getRecieptNo()))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    }

    private String paymentPayerName(Payment payment) {
        String bookingNumber = bookingNumberFromRemark(payment == null ? null : payment.getRemark());
        if (bookingNumber != null) {
            return bookings.findByBookingNumber(bookingNumber)
                    .map(com.hotel.model.Booking::getCustomerName)
                    .filter(name -> name != null && !name.isBlank())
                    .orElseGet(() -> bookingPayerNameFromRemark(payment.getRemark()));
        }
        String payerNameFromRemark = bookingPayerNameFromRemark(payment == null ? null : payment.getRemark());
        if (!"-".equals(payerNameFromRemark)) {
            return payerNameFromRemark;
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

    private String bookingPayerNameFromRemark(String remark) {
        if (remark == null || remark.isBlank()) {
            return "-";
        }
        int marker = remark.lastIndexOf(':');
        if (marker < 0 || marker + 1 >= remark.length()) {
            return "-";
        }
        String value = remark.substring(marker + 1).trim();
        return value.isBlank() ? "-" : value;
    }

    public record DateRange(LocalDate start, LocalDate end) {
        public LocalDateTime startDateTime() {
            return start.atStartOfDay();
        }

        public LocalDateTime endExclusiveDateTime() {
            return end.plusDays(1).atStartOfDay();
        }
    }

    public record RevenueReport(List<Payment> payments,
                                Map<Long, String> payerNames,
                                Map<Long, String> receiptNumbers,
                                BigDecimal totalRevenue,
                                BigDecimal fineRevenue,
                                Map<String, BigDecimal> revenueByType,
                                Map<String, BigDecimal> revenueByMethod) {
    }

    public record MonthlyBillReport(List<com.hotel.model.MonthlyRentBill> monthlyBills, BigDecimal outstandingAmount) {
    }

    public record RoomReport(long totalRooms, long availableRooms, long occupiedRooms, Map<String, Long> roomStatusCounts) {
    }

    public record BookingReport(List<com.hotel.model.Booking> bookings) {
    }

    public record DepositRefundReport(List<com.hotel.model.DepositRefund> depositRefunds, BigDecimal refundAmount) {
    }
}
