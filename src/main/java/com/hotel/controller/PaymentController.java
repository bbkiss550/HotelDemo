package com.hotel.controller;

import com.hotel.model.Payment;
import com.hotel.model.PaymentStatus;
import com.hotel.model.MonthlyRentBill;
import com.hotel.model.MonthlyRentBillStatus;
import com.hotel.model.RecieptType;
import com.hotel.model.StayType;
import com.hotel.repository.BillStatusRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.MonthlyRentBillRepository;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.RecieptTypeRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.AppSettingService;
import com.hotel.service.AuditService;
import com.hotel.service.RecieptRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentRepository payments;
    private final MonthlyRentBillRepository monthlyBills;
    private final BillStatusRepository billStatuses;
    private final RoomRepository rooms;
    private final GuestRepository guests;
    private final RecieptTypeRepository recieptTypes;
    private final AppSettingService settings;
    private final RecieptRecordService recieptRecordService;
    private final AuditService audit;

    public PaymentController(PaymentRepository payments, MonthlyRentBillRepository monthlyBills, BillStatusRepository billStatuses, RoomRepository rooms, GuestRepository guests, RecieptTypeRepository recieptTypes, AppSettingService settings, RecieptRecordService recieptRecordService, AuditService audit) {
        this.payments = payments;
        this.monthlyBills = monthlyBills;
        this.billStatuses = billStatuses;
        this.rooms = rooms;
        this.guests = guests;
        this.recieptTypes = recieptTypes;
        this.settings = settings;
        this.recieptRecordService = recieptRecordService;
        this.audit = audit;
    }

    @GetMapping
    String index(@RequestParam(required = false) Integer month,
                 @RequestParam(required = false) Integer year,
                 @RequestParam(defaultValue = "ALL") String status,
                 @RequestParam(defaultValue = "") String billNo,
                 @RequestParam(defaultValue = "") String roomNo,
                 @RequestParam(defaultValue = "") String guestName,
                 Model model) {
        YearMonth selectedPeriod = normalizePeriod(month, year);
        Integer selectedMonth = month == null || month < 1 || month > 12 ? null : selectedPeriod.getMonthValue();
        Integer selectedYear = year == null || year < 2000 ? null : selectedPeriod.getYear();
        var paymentList = payments.findAllOrderByReceiptNoDesc();
        Map<Long, String> paymentBillNumbers = paymentList.stream()
                .collect(Collectors.toMap(Payment::getId, this::monthlyRentBillNumber, (left, right) -> left));
        Map<Long, MonthlyRentBill> paymentBills = paymentList.stream()
                .map(payment -> new AbstractMap.SimpleEntry<>(payment.getId(), monthlyRentBill(payment)))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
        Map<Long, MonthlyRentBillSlipItem> paymentBillItems = paymentBills.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new MonthlyRentBillSlipItem(entry.getValue()), (left, right) -> left));
        Map<Long, List<ReceiptLine>> paymentReceiptItems = paymentList.stream()
                .collect(Collectors.toMap(Payment::getId, this::receiptItems, (left, right) -> left));
        Map<Long, String> paymentReceiptNumbers = paymentList.stream()
                .collect(Collectors.toMap(Payment::getId, payment -> payment.getReciept() != null
                        ? payment.getReciept().getRecieptNo()
                        : null, (left, right) -> left));
        List<MonthlyRentBill> payableBills = monthlyBills.findByStatusIdInOrderByDueDateAscIdAsc(List.of(
                        MonthlyRentBillStatus.PENDING.getId(),
                        MonthlyRentBillStatus.PARTIAL_PAID.getId()))
                .stream()
                .filter(bill -> selectedMonth == null || selectedMonth.equals(bill.getBillingMonth()))
                .filter(bill -> selectedYear == null || selectedYear.equals(bill.getBillingYear()))
                .filter(bill -> matchesStatus(bill, status))
                .filter(bill -> money(bill.getRemainingAmount()).compareTo(BigDecimal.ZERO) > 0)
                .filter(bill -> matchesBillSearch(bill, billNo, roomNo, guestName))
                .sorted(Comparator.comparing(MonthlyRentBill::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MonthlyRentBill::getId))
                .toList();
        model.addAttribute("payableBills", payableBills);
        model.addAttribute("payments", paymentList);
        model.addAttribute("paymentBillNumbers", paymentBillNumbers);
        model.addAttribute("paymentBills", paymentBills);
        model.addAttribute("paymentBillItems", paymentBillItems);
        model.addAttribute("paymentReceiptItems", paymentReceiptItems);
        model.addAttribute("paymentReceiptNumbers", paymentReceiptNumbers);
        model.addAttribute("paymentReceiptTypeNames", paymentReceiptTypeNames(paymentList));
        model.addAttribute("rooms", rooms.findAllByOrderByRoomNumber());
        model.addAttribute("guests", guests.findByActiveTrueOrderByCheckInDateDescIdDesc());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        model.addAttribute("recieptTypes", recieptTypes.findAll());
        model.addAttribute("billingMonth", selectedMonth);
        model.addAttribute("billingYear", selectedYear);
        model.addAttribute("months", monthOptions());
        model.addAttribute("years", years());
        model.addAttribute("statuses", billStatuses.findAllByOrderByIdAsc());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("billNo", billNo);
        model.addAttribute("roomNo", roomNo);
        model.addAttribute("guestName", guestName);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("defaultFineAmount", settings.fineAmount());
        model.addAttribute("fineIntervalDays", settings.fineIntervalDays());
        return "payments/index";
    }

    @PostMapping("/monthly-bill/{billId}")
    @Transactional
    String payMonthlyBill(@PathVariable Long billId,
                          @RequestParam BigDecimal amount,
                          @RequestParam(defaultValue = "0") BigDecimal fineAmount,
                          @RequestParam(required = false) LocalDate paymentDate,
                          @RequestParam(defaultValue = "เงินสด") String paymentMethod,
                          @RequestParam(defaultValue = "") String remark,
                          RedirectAttributes redirect) {
        MonthlyRentBill bill = monthlyBills.findById(billId).orElseThrow();
        BigDecimal paidAmount = money(amount);
        LocalDate effectivePaymentDate = paymentDate == null ? LocalDate.now() : paymentDate;
        BigDecimal penaltyAmount = calculateFineAmount(bill.getDueDate(), effectivePaymentDate);
        BigDecimal remainingAmount = money(bill.getRemainingAmount());

        if (bill.getStatus() == MonthlyRentBillStatus.CANCELLED || bill.getStatus() == MonthlyRentBillStatus.PAID) {
            redirect.addFlashAttribute("error", "บิลนี้ไม่อยู่ในสถานะที่รับชำระได้");
            redirect.addFlashAttribute("flashType", "warning");
            return "redirect:/payments";
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            redirect.addFlashAttribute("error", "ยอดชำระต้องมากกว่า 0");
            redirect.addFlashAttribute("flashType", "warning");
            return "redirect:/payments";
        }
        if (paidAmount.compareTo(remainingAmount) > 0) {
            redirect.addFlashAttribute("error", "ยอดชำระมากกว่ายอดคงเหลือ");
            redirect.addFlashAttribute("flashType", "warning");
            return "redirect:/payments";
        }
        Payment payment = new Payment();
        payment.setRoom(bill.getRoom());
        payment.setGuest(bill.getGuest());
        payment.setAmount(paidAmount);
        payment.setFineAmount(penaltyAmount);
        payment.setPaymentDate(effectivePaymentDate);
        payment.setPaymentMethod(paymentMethod == null || paymentMethod.isBlank() ? "เงินสด" : paymentMethod);
        payment.setStatus(PaymentStatus.PAID);
        payment.setMonthlyRentBill(bill);
        String billReference = "ชำระบิลค่าเช่ารายเดือน #" + bill.getDisplayBillNumber();
        if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
            billReference += " ค่าปรับ " + penaltyAmount;
        }
        payment.setRemark(remark == null || remark.isBlank() ? billReference : billReference + " - " + remark);
        payment = payments.save(payment);

        bill.setPaidAmount(money(bill.getPaidAmount()).add(paidAmount));
        bill.recalculate();
        bill.setStatus(money(bill.getRemainingAmount()).compareTo(BigDecimal.ZERO) == 0
                ? MonthlyRentBillStatus.PAID
                : MonthlyRentBillStatus.PARTIAL_PAID);
        monthlyBills.save(bill);

        if (bill.getGuest() != null) {
            var guest = bill.getGuest();
            guest.setTotalPaid(money(guest.getTotalPaid()).add(paidAmount));
            guests.save(guest);
        }
        recieptRecordService.recordMonthlyRent(payment);
        payments.save(payment);
        audit.record("MONTHLY_RENT_PAYMENT", "Bill " + bill.getDisplayBillNumber() + " amount " + paidAmount);
        redirect.addFlashAttribute("message", "บันทึกชำระเงินเรียบร้อย");
        redirect.addFlashAttribute("flashType", "success");
        redirect.addFlashAttribute("autoReceiptPaymentId", payment.getId());
        return "redirect:/payments";
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
    @Transactional
    String save(@ModelAttribute Payment payment,
                @RequestParam(required = false) Long guestId,
                @RequestParam Long roomId,
                @RequestParam(defaultValue = "2") Long recieptTypeId,
                RedirectAttributes redirect) {
        boolean isNew = payment.getId() == null;
        payment.setRoom(rooms.findById(roomId).orElseThrow());
        if (guestId != null) {
            payment.setGuest(guests.findById(guestId).orElse(null));
        }
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
        }
        payment = payments.save(payment);
        if (isNew && payment.getGuest() != null && payment.getStatus() == PaymentStatus.PAID) {
            var guest = payment.getGuest();
            BigDecimal totalPaid = guest.getTotalPaid() == null ? BigDecimal.ZERO : guest.getTotalPaid();
            guest.setTotalPaid(totalPaid.add(payment.getTotalAmount()));
            guests.save(guest);
            recieptRecordService.record(recieptTypeId == null ? RecieptType.DAILY_SERVICE : recieptTypeId, payment.getTotalAmount(), payment);
            payments.save(payment);
            redirect.addFlashAttribute("autoReceiptPaymentId", payment.getId());
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
        model.addAttribute("recieptTypes", recieptTypes.findAll());
    }

    private boolean matchesBillSearch(MonthlyRentBill bill, String billNo, String roomNo, String guestName) {
        return containsIfPresent(bill.getBillNumber(), billNo)
                && containsIfPresent(bill.getRoom() == null ? null : bill.getRoom().getRoomNumber(), roomNo)
                && containsIfPresent(bill.getGuest() == null ? null : bill.getGuest().getFullName(), guestName);
    }

    private boolean containsIfPresent(String value, String term) {
        return term == null || term.isBlank()
                || (value != null && value.toLowerCase(java.util.Locale.ROOT).contains(term.toLowerCase(java.util.Locale.ROOT)));
    }

    private boolean matchesStatus(MonthlyRentBill bill, String status) {
        if (status == null || status.isBlank() || "ALL".equals(status)) {
            return true;
        }
        try {
            return Long.valueOf(status).equals(bill.getStatusId());
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private YearMonth normalizePeriod(Integer month, Integer year) {
        YearMonth fallback = YearMonth.now().minusMonths(1);
        int normalizedMonth = month == null || month < 1 || month > 12 ? fallback.getMonthValue() : month;
        int normalizedYear = year == null || year < 2000 ? fallback.getYear() : year;
        return YearMonth.of(normalizedYear, normalizedMonth);
    }

    private List<MonthOption> monthOptions() {
        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new MonthOption(month, thaiMonth(month)))
                .toList();
    }

    private String thaiMonth(int month) {
        return List.of("มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
                "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม").get(month - 1);
    }

    private List<Integer> years() {
        int current = LocalDate.now().getYear();
        return java.util.stream.IntStream.rangeClosed(current, current + 3)
                .boxed()
                .toList();
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(term);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

    private MonthlyRentBill monthlyRentBill(Payment payment) {
        if (payment != null && payment.getMonthlyRentBill() != null) {
            return payment.getMonthlyRentBill();
        }
        String billNumber = monthlyRentBillNumber(payment);
        if ("-".equals(billNumber)) {
            return null;
        }
        return monthlyBills.findByBillNumber(billNumber).orElse(null);
    }

    private BigDecimal calculateFineAmount(LocalDate dueDate, LocalDate paymentDate) {
        if (dueDate == null || paymentDate == null || !paymentDate.isAfter(dueDate)) {
            return BigDecimal.ZERO;
        }
        long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(dueDate, paymentDate);
        int intervalDays = Math.max(settings.fineIntervalDays(), 1);
        long periods = Math.max(1, (overdueDays + intervalDays - 1) / intervalDays);
        return settings.fineAmount().multiply(BigDecimal.valueOf(periods));
    }

    private Map<Long, String> paymentReceiptTypeNames(List<Payment> paymentList) {
        return paymentList.stream().collect(Collectors.toMap(Payment::getId, payment -> payment.getReciept() != null && payment.getReciept().getType() != null
                ? payment.getReciept().getType().getName()
                : "-", (left, right) -> left));
    }

    public record MonthOption(Integer value, String label) {
    }

    public record MonthlyRentBillSlipItem(MonthlyRentBill bill) {
    }

    private List<ReceiptLine> receiptItems(Payment payment) {
        if (payment == null || payment.getGuest() == null || payment.getGuest().getStayType() != StayType.DAILY) {
            return List.of(new ReceiptLine(paymentReceiptTypeName(payment), BigDecimal.ONE, payment == null ? BigDecimal.ZERO : payment.getAmount(), payment == null ? BigDecimal.ZERO : payment.getAmount()));
        }
        var guest = payment.getGuest();
        BigDecimal price = money(guest.getPrice());
        BigDecimal deposit = money(guest.getDeposit());
        long days = 1;
        if (guest.getCheckInDate() != null && guest.getCheckOutDate() != null) {
            days = Math.max(1, ChronoUnit.DAYS.between(guest.getCheckInDate(), guest.getCheckOutDate()));
        }
        BigDecimal roomAmount = price.multiply(BigDecimal.valueOf(days));
        BigDecimal paidAmount = money(payment.getAmount());
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

    private String paymentReceiptTypeName(Payment payment) {
        return payment != null && payment.getReciept() != null && payment.getReciept().getType() != null
                ? payment.getReciept().getType().getName()
                : "-";
    }

    public record ReceiptLine(String label, BigDecimal units, BigDecimal unitPrice, BigDecimal amount) {
    }
}
