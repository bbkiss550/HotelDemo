package com.hotel.controller;

import com.hotel.model.MonthlyRentBillStatus;

import com.hotel.model.LookupCodes;

import com.hotel.model.Payment;
import com.hotel.model.MonthlyRentBill;
import com.hotel.model.RecieptType;
import com.hotel.repository.BillStatusRepository;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.DepositRefundRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.MonthlyRentBillRepository;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.PaymentDetailRepository;
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
    private final PaymentDetailRepository paymentDetails;
    private final BookingRepository bookings;
    private final DepositRefundRepository depositRefunds;
    private final MonthlyRentBillRepository monthlyBills;
    private final BillStatusRepository billStatuses;
    private final RoomRepository rooms;
    private final GuestRepository guests;
    private final RecieptTypeRepository recieptTypes;
    private final AppSettingService settings;
    private final RecieptRecordService recieptRecordService;
    private final AuditService audit;

    public PaymentController(PaymentRepository payments, PaymentDetailRepository paymentDetails, BookingRepository bookings, DepositRefundRepository depositRefunds, MonthlyRentBillRepository monthlyBills, BillStatusRepository billStatuses, RoomRepository rooms, GuestRepository guests, RecieptTypeRepository recieptTypes, AppSettingService settings, RecieptRecordService recieptRecordService, AuditService audit) {
        this.payments = payments;
        this.paymentDetails = paymentDetails;
        this.bookings = bookings;
        this.depositRefunds = depositRefunds;
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
                 @RequestParam(defaultValue = "") String receiptNo,
                 @RequestParam(defaultValue = "") String referenceNo,
                 @RequestParam(required = false) LocalDate paymentDate,
                 @RequestParam(defaultValue = "") String paymentGuestName,
                 @RequestParam(required = false) Long receiptTypeId,
                 Model model) {
        YearMonth selectedPeriod = normalizePeriod(month, year);
        Integer selectedMonth = month == null || month < 1 || month > 12 ? null : selectedPeriod.getMonthValue();
        Integer selectedYear = year == null || year < 2000 ? null : selectedPeriod.getYear();
        var paymentList = payments.findAllOrderByReceiptNoDesc()
                .stream()
                .filter(payment -> containsIfPresent(payment.getReciept() == null ? null : payment.getReciept().getRecieptNo(), receiptNo))
                .filter(payment -> containsIfPresent(monthlyRentBillNumber(payment), referenceNo))
                .filter(payment -> paymentDate == null || paymentDate.equals(payment.getPaymentDate()))
                .filter(payment -> containsIfPresent(payment.getGuest() == null ? null : payment.getGuest().getFullName(), paymentGuestName))
                .filter(payment -> receiptTypeId == null
                        || (payment.getReciept() != null
                        && payment.getReciept().getType() != null
                        && receiptTypeId.equals(payment.getReciept().getType().getId())))
                .toList();
        Map<Long, String> paymentBillNumbers = paymentList.stream()
                .collect(Collectors.toMap(Payment::getId, this::monthlyRentBillNumber, (left, right) -> left));
        Map<Long, String> paymentPayerNames = paymentList.stream()
                .collect(Collectors.toMap(Payment::getId, this::paymentPayerName, (left, right) -> left));
        Map<Long, String> paymentReceiptNumbers = paymentList.stream()
                .map(payment -> new AbstractMap.SimpleEntry<>(payment.getId(), payment.getReciept() == null
                        ? null
                        : payment.getReciept().getRecieptNo()))
                .filter(entry -> entry.getValue() != null && !entry.getValue().isBlank())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
        List<MonthlyRentBill> payableBills = monthlyBills.findByStatusIdInOrderByDueDateAscIdAsc(List.of(
                        MonthlyRentBillStatus.getId(MonthlyRentBillStatus.PENDING),
                        MonthlyRentBillStatus.getId(MonthlyRentBillStatus.PARTIAL_PAID)))
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
        model.addAttribute("paymentPayerNames", paymentPayerNames);
        model.addAttribute("paymentReceiptNumbers", paymentReceiptNumbers);
        model.addAttribute("paymentReceiptTypeNames", paymentReceiptTypeNames(paymentList));
        model.addAttribute("rooms", rooms.findAllByOrderByRoomNumber());
        model.addAttribute("guests", guests.findByActiveTrueOrderByCheckInDateDescIdDesc());
        model.addAttribute("paymentStatuses", LookupCodes.paymentStatusCodes());
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
        model.addAttribute("receiptNo", receiptNo);
        model.addAttribute("referenceNo", referenceNo);
        model.addAttribute("paymentDate", paymentDate);
        model.addAttribute("paymentGuestName", paymentGuestName);
        model.addAttribute("receiptTypeId", receiptTypeId);
        model.addAttribute("billFilterApplied", month != null
                || year != null
                || !"ALL".equals(status)
                || !billNo.isBlank()
                || !roomNo.isBlank()
                || !guestName.isBlank());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("defaultFineAmount", settings.fineAmount());
        model.addAttribute("fineIntervalDays", settings.fineIntervalDays());
        return "payments/index";
    }

    @GetMapping("/{id}/edit-modal")
    String editModal(@PathVariable Long id, Model model) {
        Payment payment = payments.findById(id).orElseThrow();
        addPaymentModalAttributes(model, payment);
        return "payments/modal-fragments :: editModal";
    }

    @GetMapping("/{id}/receipt-modal")
    String receiptModal(@PathVariable Long id, Model model) {
        Payment payment = payments.findById(id).orElseThrow();
        addPaymentModalAttributes(model, payment);
        return "payments/modal-fragments :: receiptModal";
    }

    @GetMapping("/monthly-bill/{billId}/payment-modal")
    String monthlyBillPaymentModal(@PathVariable Long billId, Model model) {
        model.addAttribute("bill", monthlyBills.findById(billId).orElseThrow());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("defaultFineAmount", settings.fineAmount());
        model.addAttribute("fineIntervalDays", settings.fineIntervalDays());
        return "payments/modal-fragments :: monthlyBillPaymentModal";
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

        if (MonthlyRentBillStatus.CANCELLED.equals(bill.getStatus()) || MonthlyRentBillStatus.PAID.equals(bill.getStatus())) {
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
        payment.setStatus(LookupCodes.PAID);
        payment.setMonthlyRentBill(bill);
        payment.setRemark(remark == null || remark.isBlank() ? null : remark.trim());
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
                payment.setStatus(LookupCodes.PAID);
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
    String save(@ModelAttribute Payment paymentForm,
                @RequestParam(required = false) Long guestId,
                @RequestParam(required = false) Long roomId,
                @RequestParam(defaultValue = "2") Long recieptTypeId,
                RedirectAttributes redirect) {
        boolean isNew = paymentForm.getId() == null;
        Payment payment = isNew
                ? paymentForm
                : payments.findById(paymentForm.getId()).orElseThrow();

        payment.setRoom(roomId == null ? null : rooms.findById(roomId).orElseThrow());
        payment.setGuest(guestId == null ? null : guests.findById(guestId).orElse(null));
        payment.setAmount(money(paymentForm.getAmount()));
        payment.setFineAmount(money(paymentForm.getFineAmount()));
        payment.setPaymentDate(paymentForm.getPaymentDate() == null ? LocalDate.now() : paymentForm.getPaymentDate());
        payment.setPaymentMethod(paymentForm.getPaymentMethod() == null || paymentForm.getPaymentMethod().isBlank()
                ? "เงินสด"
                : paymentForm.getPaymentMethod());
        payment.setRemark(paymentForm.getRemark());
        payment.setStatus(paymentForm.getStatus() == null ? LookupCodes.PAID : paymentForm.getStatus());

        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDate.now());
        }
        if (!isNew && payment.getReciept() != null) {
            payment.getReciept().setAmount(payment.getTotalAmount());
        }
        payment = payments.save(payment);
        if (isNew && payment.getGuest() != null && LookupCodes.PAID.equals(payment.getStatus())) {
            var guest = payment.getGuest();
            BigDecimal totalPaid = guest.getTotalPaid() == null ? BigDecimal.ZERO : guest.getTotalPaid();
            guest.setTotalPaid(totalPaid.add(payment.getTotalAmount()));
            guests.save(guest);
            recieptRecordService.record(recieptTypeId == null ? RecieptType.DAILY_SERVICE : recieptTypeId, payment.getTotalAmount(), payment);
            payments.save(payment);
            redirect.addFlashAttribute("autoReceiptPaymentId", payment.getId());
        }
        audit.record("PAYMENT", "Room " + (payment.getRoom() == null ? "-" : payment.getRoom().getRoomNumber()) + " amount " + payment.getAmount());
        redirect.addFlashAttribute("message", (isNew ? "บันทึกการชำระเงิน" : "แก้ไขการชำระเงิน") + "เรียบร้อย");
        redirect.addFlashAttribute("flashType", isNew ? "success" : "edit");
        return "redirect:/payments";
    }

    private void formData(Model model, Payment payment) {
        model.addAttribute("payment", payment);
        model.addAttribute("rooms", rooms.findAllByOrderByRoomNumber());
        model.addAttribute("guests", guests.findByActiveTrueOrderByCheckInDateDescIdDesc());
        model.addAttribute("statuses", LookupCodes.paymentStatusCodes());
        model.addAttribute("recieptTypes", recieptTypes.findAll());
    }

    private void addPaymentModalAttributes(Model model, Payment payment) {
        MonthlyRentBill bill = monthlyRentBill(payment);
        model.addAttribute("p", payment);
        model.addAttribute("receiptNo", payment.getReciept() != null ? payment.getReciept().getRecieptNo() : null);
        model.addAttribute("billNumber", monthlyRentBillNumber(payment));
        model.addAttribute("billItem", null);
        model.addAttribute("receiptItems", receiptItems(payment));
        model.addAttribute("receiptTypeName", paymentReceiptTypeName(payment));
        model.addAttribute("payerName", paymentPayerName(payment));
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
        if (payment == null || payment.getId() == null) {
            return List.of();
        }
        return paymentDetails.findByPaymentIdOrderBySortOrderAscIdAsc(payment.getId()).stream()
                .map(detail -> new ReceiptLine(
                        detail.getItem().getName(),
                        detail.getItem().getNameEn(),
                        detail.getQuantity(),
                        detail.getUnitPrice(),
                        detail.getAmount()))
                .toList();
    }

    private List<ReceiptLine> legacyReceiptItems(Payment payment) {
        if (isPenaltyReceipt(payment)) {
            return checkoutPenaltyReceiptItems(payment);
        }
        if (payment == null || payment.getGuest() == null || !LookupCodes.DAILY.equals(payment.getGuest().getStayType())) {
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

    private List<ReceiptLine> checkoutPenaltyReceiptItems(Payment payment) {
        var refund = payment.getDepositRefund() != null
                ? payment.getDepositRefund()
                : depositRefunds.findByRefundNo(refundNumberFromRemark(payment.getRemark())).orElse(null);
        if (refund == null) {
            return List.of(new ReceiptLine(paymentReceiptTypeName(payment), BigDecimal.ONE, money(payment.getAmount()), money(payment.getAmount())));
        }
        List<ReceiptLine> items = refund.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getSortOrder() == null ? 0 : item.getSortOrder()))
                .map(item -> checkoutPenaltyReceiptLine(payment, item))
                .collect(Collectors.toCollection(ArrayList::new));
        BigDecimal deductedDeposit = money(refund.getDepositAmount()).min(money(refund.getTotalDeductAmount()));
        if (deductedDeposit.compareTo(BigDecimal.ZERO) > 0) {
            items.add(new ReceiptLine("หักค่าประกัน", "Deposit deduction", BigDecimal.ONE, deductedDeposit.negate(), deductedDeposit.negate()));
        }
        if (items.isEmpty()) {
            items.add(new ReceiptLine(paymentReceiptTypeName(payment), BigDecimal.ONE, money(payment.getAmount()), money(payment.getAmount())));
        }
        return items;
    }

    private ReceiptLine checkoutPenaltyReceiptLine(Payment payment, com.hotel.model.DepositRefundItem item) {
        String label = withoutPenaltyTimeRange(item.getItemName());
        BigDecimal amount = money(item.getItemAmount());
        if (isAdditionalNightCharge(label) && payment.getGuest() != null) {
            BigDecimal nightlyPrice = money(payment.getGuest().getPrice());
            if (nightlyPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal nights = amount.divide(nightlyPrice, 0, java.math.RoundingMode.HALF_UP).max(BigDecimal.ONE);
                return new ReceiptLine(label, itemEnglishLabel(item), nights, nightlyPrice, amount);
            }
        }
        return new ReceiptLine(label, itemEnglishLabel(item), BigDecimal.ONE, amount, amount);
    }

    private boolean isAdditionalNightCharge(String label) {
        return label != null && (label.contains("เพิ่มค่าห้องเป็นจำนวนคืน")
                || label.contains("คิดค่าห้องเพิ่มตามจำนวนคืน"));
    }

    private String itemEnglishLabel(com.hotel.model.DepositRefundItem item) {
        if (item.getItemNameEn() != null && !item.getItemNameEn().isBlank()) {
            return item.getItemNameEn();
        }
        String label = item.getItemName() == null ? "" : item.getItemName();
        if (label.contains("เพิ่มค่าห้องเป็นจำนวนคืน") || label.contains("คิดค่าห้องเพิ่มตามจำนวนคืน")) {
            return "Late check-out charge additional night";
        }
        if (label.startsWith("ค่าปรับเช็คเอาท์")) {
            return "Late checkout penalty";
        }
        if (label.startsWith("ค่าเสียหาย")) {
            return "Damage charge";
        }
        return "Deduction item";
    }

    private String withoutPenaltyTimeRange(String label) {
        if (label == null) {
            return "-";
        }
        return label.replaceAll("\\s*\\(\\d{1,2}:\\d{2}(?::\\d{2})?\\s*-\\s*\\d{1,2}:\\d{2}(?::\\d{2})?\\)", "").trim();
    }

    private boolean isPenaltyReceipt(Payment payment) {
        return payment != null && payment.getReciept() != null && payment.getReciept().getType() != null
                && RecieptType.PENALTY == payment.getReciept().getType().getId();
    }

    private String refundNumberFromRemark(String remark) {
        String marker = "อ้างอิงใบสำคัญ ";
        if (remark == null) {
            return "";
        }
        int start = remark.indexOf(marker);
        return start < 0 ? "" : remark.substring(start + marker.length()).trim().split("\\s+", 2)[0];
    }

    private String paymentReceiptTypeName(Payment payment) {
        return payment != null && payment.getReciept() != null && payment.getReciept().getType() != null
                ? payment.getReciept().getType().getName()
                : "-";
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

    public record ReceiptLine(String label, String englishLabel, BigDecimal units, BigDecimal unitPrice, BigDecimal amount) {
        public ReceiptLine(String label, BigDecimal units, BigDecimal unitPrice, BigDecimal amount) {
            this(label, null, units, unitPrice, amount);
        }
    }
}
