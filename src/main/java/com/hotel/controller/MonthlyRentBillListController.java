package com.hotel.controller;

import com.hotel.model.MonthlyRentBillStatus;

import com.hotel.model.MonthlyRentBill;
import com.hotel.repository.BillStatusRepository;
import com.hotel.repository.MonthlyRentBillRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/finance/monthly-rent/bills")
public class MonthlyRentBillListController {
    private final MonthlyRentBillRepository bills;
    private final BillStatusRepository billStatuses;

    public MonthlyRentBillListController(MonthlyRentBillRepository bills, BillStatusRepository billStatuses) {
        this.bills = bills;
        this.billStatuses = billStatuses;
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

        List<MonthlyRentBillItem> items = bills.findAllByOrderByBillingYearDescBillingMonthDescIdDesc().stream()
                .filter(bill -> selectedMonth == null || bill.getBillingMonth().equals(selectedMonth))
                .filter(bill -> selectedYear == null || bill.getBillingYear().equals(selectedYear))
                .filter(bill -> matchesStatus(bill, status))
                .filter(bill -> matchesSearch(bill, billNo, roomNo, guestName))
                .map(MonthlyRentBillItem::new)
                .toList();

        model.addAttribute("bills", items);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("billNo", billNo);
        model.addAttribute("roomNo", roomNo);
        model.addAttribute("guestName", guestName);
        model.addAttribute("months", monthOptions());
        model.addAttribute("years", years());
        model.addAttribute("statuses", billStatuses.findAllByOrderByIdAsc());
        return "finance/monthly-rent/bills";
    }

    private YearMonth normalizePeriod(Integer month, Integer year) {
        YearMonth fallback = YearMonth.now().minusMonths(1);
        int normalizedMonth = month == null || month < 1 || month > 12 ? fallback.getMonthValue() : month;
        int normalizedYear = year == null || year < 2000 ? fallback.getYear() : year;
        return YearMonth.of(normalizedYear, normalizedMonth);
    }

    private boolean matchesStatus(MonthlyRentBill bill, String status) {
        if (status == null || status.isBlank() || "ALL".equals(status)) {
            return true;
        }
        try {
            return "OVERDUE".equals(status)
                    ? MonthlyRentBillStatus.OVERDUE.equals(effectiveStatus(bill))
                    : Long.valueOf(status).equals(bill.getStatusId());
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private boolean matchesSearch(MonthlyRentBill bill, String billNo, String roomNo, String guestName) {
        return containsIfPresent(bill.getBillNumber(), billNo)
                && containsIfPresent(bill.getRoom() == null ? null : bill.getRoom().getRoomNumber(), roomNo)
                && containsIfPresent(bill.getGuest() == null ? null : bill.getGuest().getFullName(), guestName);
    }

    private boolean containsIfPresent(String value, String term) {
        return term == null || term.isBlank()
                || (value != null && value.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT)));
    }

    private String effectiveStatus(MonthlyRentBill bill) {
        if (!MonthlyRentBillStatus.PAID.equals(bill.getStatus())
                && !MonthlyRentBillStatus.CANCELLED.equals(bill.getStatus())
                && bill.getDueDate() != null
                && bill.getDueDate().isBefore(LocalDate.now())) {
            return MonthlyRentBillStatus.OVERDUE;
        }
        return bill.getStatus();
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

    public class MonthlyRentBillItem {
        private final MonthlyRentBill bill;

        MonthlyRentBillItem(MonthlyRentBill bill) {
            this.bill = bill;
        }

        public MonthlyRentBill getBill() {
            return bill;
        }

        public String getEffectiveStatus() {
            return effectiveStatus(bill);
        }

        public String getStatusLabel() {
            String status = getEffectiveStatus();
            if (MonthlyRentBillStatus.OVERDUE.equals(status)) {
                return MonthlyRentBillStatus.label(status);
            }
            return bill.getBillStatus() == null ? MonthlyRentBillStatus.label(status) : bill.getBillStatus().getName();
        }

        public String getStatusClass() {
            return switch (getEffectiveStatus()) {
                case "DRAFT" -> "bg-light-secondary";
                case "PENDING" -> "bg-light-warning";
                case "PARTIAL_PAID" -> "bg-light-primary";
                case "PAID" -> "bg-light-success";
                case "OVERDUE" -> "bg-light-danger";
                case "CANCELLED" -> "bg-light-dark";
                default -> "bg-light-secondary";
            };
        }
    }

    public record MonthOption(Integer value, String label) {
    }
}
