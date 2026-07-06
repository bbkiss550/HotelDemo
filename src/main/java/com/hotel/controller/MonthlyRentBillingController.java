package com.hotel.controller;

import com.hotel.model.Guest;
import com.hotel.model.MonthlyRentBill;
import com.hotel.model.MonthlyRentBillStatus;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.StayType;
import com.hotel.repository.BillStatusRepository;
import com.hotel.repository.FloorRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.AdvanceLedgerRepository;
import com.hotel.repository.MonthlyRentBillRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.AdvanceBalanceService;
import com.hotel.service.AppSettingService;
import com.hotel.service.AuditService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/finance/monthly-rent/billing")
public class MonthlyRentBillingController {
    private static final List<MonthlyRentBillStatus> ISSUED_STATUSES = List.of(
            MonthlyRentBillStatus.PENDING,
            MonthlyRentBillStatus.PARTIAL_PAID,
            MonthlyRentBillStatus.PAID,
            MonthlyRentBillStatus.OVERDUE
    );

    private final MonthlyRentBillRepository bills;
    private final BillStatusRepository billStatuses;
    private final GuestRepository guests;
    private final RoomRepository rooms;
    private final FloorRepository floors;
    private final AdvanceLedgerRepository advanceLedgers;
    private final AdvanceBalanceService advanceBalanceService;
    private final AppSettingService settings;
    private final AuditService audit;

    public MonthlyRentBillingController(MonthlyRentBillRepository bills, BillStatusRepository billStatuses, GuestRepository guests, RoomRepository rooms, FloorRepository floors, AdvanceLedgerRepository advanceLedgers, AdvanceBalanceService advanceBalanceService, AppSettingService settings, AuditService audit) {
        this.bills = bills;
        this.billStatuses = billStatuses;
        this.guests = guests;
        this.rooms = rooms;
        this.floors = floors;
        this.advanceLedgers = advanceLedgers;
        this.advanceBalanceService = advanceBalanceService;
        this.settings = settings;
        this.audit = audit;
    }

    @GetMapping
    String index(@RequestParam(required = false) Integer month,
                 @RequestParam(required = false) Integer year,
                 @RequestParam(required = false) Long floorId,
                 @RequestParam(defaultValue = "ALL") String status,
                 @RequestParam(defaultValue = "") String billNo,
                 @RequestParam(defaultValue = "") String roomNo,
                 @RequestParam(defaultValue = "") String guestName,
                 Model model) {
        addBillingPageData(month, year, floorId, status, billNo, roomNo, guestName, model);
        return "finance/monthly-rent/billing";
    }

    @GetMapping("/content")
    String content(@RequestParam(required = false) Integer month,
                   @RequestParam(required = false) Integer year,
                   @RequestParam(required = false) Long floorId,
                   @RequestParam(defaultValue = "ALL") String status,
                   @RequestParam(defaultValue = "") String billNo,
                   @RequestParam(defaultValue = "") String roomNo,
                   @RequestParam(defaultValue = "") String guestName,
                   Model model) {
        addBillingPageData(month, year, floorId, status, billNo, roomNo, guestName, model);
        return "finance/monthly-rent/billing :: monthlyBillingWorkspace";
    }

    private void addBillingPageData(Integer month, Integer year, Long floorId, String status, String billNo, String roomNo, String guestName, Model model) {
        YearMonth period = normalizePeriod(month, year);
        var floorList = floors.findAllByOrderBySortOrderAscNumberAscNameAsc();
        var selectedFloor = floorId == null
                ? floorList.stream().findFirst().orElse(null)
                : floors.findById(floorId).orElseGet(() -> floorList.stream().findFirst().orElse(null));

        List<BillingRoomItem> allItems = monthlyActiveItems(period);
        List<BillingRoomItem> filteredItems = allItems.stream()
                .filter(item -> isPreviewItem(item, model)
                        || ((selectedFloor == null || (item.getRoom().getFloor() != null && item.getRoom().getFloor().getId().equals(selectedFloor.getId())))
                        && matchesStatus(item, status)
                        && matchesSearch(item, billNo, roomNo, guestName)))
                .sorted(Comparator.comparing(item -> item.getRoom().getRoomNumber(), String.CASE_INSENSITIVE_ORDER))
                .toList();

        model.addAttribute("billingMonth", period.getMonthValue());
        model.addAttribute("billingYear", period.getYear());
        model.addAttribute("months", monthOptions());
        model.addAttribute("years", years());
        model.addAttribute("floors", floorList);
        model.addAttribute("selectedFloor", selectedFloor);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("billNo", billNo);
        model.addAttribute("roomNo", roomNo);
        model.addAttribute("guestName", guestName);
        model.addAttribute("items", filteredItems);
        model.addAttribute("statuses", billStatuses.findAllByOrderByIdAsc());
        model.addAttribute("availableCount", allItems.stream().filter(item -> item.getBill() == null || item.getBill().getStatus() == MonthlyRentBillStatus.DRAFT).count());
        model.addAttribute("printableBillCount", filteredItems.stream().filter(item -> isPrintableBill(item.getBill())).count());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("defaultDueDate", LocalDate.now().plusDays(5));
    }

    private boolean isPrintableBill(MonthlyRentBill bill) {
        return bill != null && bill.getStatus() != MonthlyRentBillStatus.DRAFT && bill.getStatus() != MonthlyRentBillStatus.CANCELLED;
    }

    private boolean isPreviewItem(BillingRoomItem item, Model model) {
        if (item.getBill() == null || item.getBill().getId() == null) {
            return false;
        }
        Long billId = item.getBill().getId();
        Object previewBillId = model.asMap().get("previewBillId");
        if (previewBillId instanceof Number number && billId.equals(number.longValue())) {
            return true;
        }
        return false;
    }

    @PostMapping
    @Transactional
    String save(@RequestParam Map<String, String> params, RedirectAttributes redirect) {
        YearMonth period = normalizePeriod(parseInt(params.get("billingMonth")), parseInt(params.get("billingYear")));
        String action = params.getOrDefault("action", "draft");
        Long roomId = parseLong(params.get("roomId"));
        Long billId = parseLong(params.get("billId"));
        if (roomId == null) {
            redirect.addFlashAttribute("error", "ไม่พบห้องสำหรับออกบิล");
            redirect.addFlashAttribute("flashType", "warning");
            return redirectToBilling(params, period);
        }

        Room room = rooms.findById(roomId).orElseThrow();
        Guest guest = guests.findTopByRoomAndActiveTrueOrderByCheckInDateDescIdDesc(room).orElse(null);
        if (!isMonthlyActive(room, guest)) {
            redirect.addFlashAttribute("error", "ห้องนี้ไม่ใช่ผู้พักรายเดือนที่ active อยู่");
            redirect.addFlashAttribute("flashType", "warning");
            return redirectToBilling(params, period);
        }

        MonthlyRentBill bill = resolveBill(room, period, billId);
        if (bill.getId() != null && bill.getStatus() == MonthlyRentBillStatus.PAID) {
            redirect.addFlashAttribute("error", "บิลที่ชำระแล้วไม่สามารถแก้ไขได้");
            redirect.addFlashAttribute("flashType", "warning");
            return redirectToBilling(params, period);
        }
        if (bill.getId() != null && billId == null && bill.getStatus() != MonthlyRentBillStatus.DRAFT) {
            redirect.addFlashAttribute("error", "ห้องนี้มีบิลในรอบที่เลือกแล้ว");
            redirect.addFlashAttribute("flashType", "warning");
            return redirectToBilling(params, period);
        }

        if ("cancel".equals(action)) {
            if (bill.getId() == null) {
                redirect.addFlashAttribute("error", "ไม่พบบิลสำหรับยกเลิก");
                redirect.addFlashAttribute("flashType", "warning");
                return redirectToBilling(params, period);
            }
            if (bill.getStatus() == MonthlyRentBillStatus.CANCELLED) {
                redirect.addFlashAttribute("error", "บิลนี้ถูกยกเลิกแล้ว");
                redirect.addFlashAttribute("flashType", "warning");
                return redirectToBilling(params, period);
            }
            if (bill.getPaidAmount() != null && bill.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                redirect.addFlashAttribute("error", "บิลนี้มีการรับชำระแล้ว ไม่สามารถยกเลิกจากหน้านี้ได้");
                redirect.addFlashAttribute("flashType", "warning");
                return redirectToBilling(params, period);
            }
            advanceBalanceService.restoreAdvanceFromCancelledBill(bill);
            advanceLedgers.findByBill(bill).forEach(ledger -> {
                ledger.setBill(null);
                advanceLedgers.save(ledger);
            });
            bills.delete(bill);
            audit.record("MONTHLY_RENT_BILL_CANCEL", "Room " + room.getRoomNumber() + " " + period);
            redirect.addFlashAttribute("message", "ยกเลิกบิลค่าเช่าแล้ว ห้องกลับเป็นยังไม่ออกบิล");
            redirect.addFlashAttribute("flashType", "delete");
            return redirectToBilling(params, period);
        }

        bindBill(bill, room, guest, period, params);
        if ("issue".equals(action)) {
            assignBillNumberIfNeeded(bill);
            bill.setStatus(MonthlyRentBillStatus.PENDING);
            bill = bills.save(bill);
            advanceBalanceService.applyAdvanceToIssuedBill(bill);
            bills.save(bill);
            redirect.addFlashAttribute("previewBillId", bill.getId());
        } else {
            bill.setAdvanceAppliedAmount(BigDecimal.ZERO);
            bill.setPaidAmount(BigDecimal.ZERO);
            bill.setStatus(MonthlyRentBillStatus.DRAFT);
            bills.save(bill);
        }
        audit.record("MONTHLY_RENT_BILL", "Room " + room.getRoomNumber() + " " + period + " " + bill.getStatus());
        redirect.addFlashAttribute("message", "issue".equals(action) ? "ออกบิลค่าเช่าเรียบร้อย" : "บันทึกแบบร่างเรียบร้อย");
        redirect.addFlashAttribute("flashType", "success");
        return redirectToBilling(params, period);
    }

    private List<BillingRoomItem> monthlyActiveItems(YearMonth period) {
        Map<Long, BillingRoomItem> items = new LinkedHashMap<>();
        for (Guest guest : guests.findByActiveTrueOrderByCheckInDateDescIdDesc()) {
            Room room = guest.getRoom();
            if (!isMonthlyActive(room, guest) || items.containsKey(room.getId())) {
                continue;
            }
            MonthlyRentBill bill = bills.findByRoomAndBillingMonthAndBillingYear(room, period.getMonthValue(), period.getYear()).orElse(null);
            MonthlyRentBill previousBill = bills.findPreviousBill(room, period.getMonthValue(), period.getYear()).stream().findFirst().orElse(null);
            items.put(room.getId(), new BillingRoomItem(room, guest, bill, previousBill, period));
        }
        return new ArrayList<>(items.values());
    }

    private boolean isMonthlyActive(Room room, Guest guest) {
        return room != null
                && guest != null
                && Boolean.TRUE.equals(guest.getActive())
                && guest.getStayType() == StayType.MONTHLY
                && (room.getStatus() == RoomStatus.MONTHLY_OCCUPIED || room.getStatus() == RoomStatus.OCCUPIED);
    }

    private boolean matchesStatus(BillingRoomItem item, String status) {
        if (status == null || status.isBlank() || "ALL".equals(status)) return true;
        if ("UNBILLED".equals(status)) return item.getBill() == null;
        if ("OVERDUE".equals(status)) return item.getEffectiveStatus() == MonthlyRentBillStatus.OVERDUE;
        Long statusId = parseLong(status);
        return statusId == null || (item.getBill() != null && statusId.equals(item.getBill().getStatusId()));
    }

    private boolean matchesSearch(BillingRoomItem item, String billNo, String roomNo, String guestName) {
        return containsIfPresent(item.getBill() == null ? null : item.getBill().getBillNumber(), billNo)
                && containsIfPresent(item.getRoom().getRoomNumber(), roomNo)
                && containsIfPresent(item.getGuest().getFullName(), guestName);
    }

    private boolean containsIfPresent(String value, String term) {
        return term == null || term.isBlank()
                || (value != null && value.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT)));
    }

    private MonthlyRentBill resolveBill(Room room, YearMonth period, Long billId) {
        if (billId != null) {
            return bills.findById(billId).orElseThrow();
        }
        return bills.findByRoomAndBillingMonthAndBillingYear(room, period.getMonthValue(), period.getYear()).orElseGet(MonthlyRentBill::new);
    }

    private void assignBillNumberIfNeeded(MonthlyRentBill bill) {
        if (bill.getBillNumber() != null && !bill.getBillNumber().isBlank()) {
            return;
        }
        LocalDate issueDate = bill.getIssueDate() == null ? LocalDate.now() : bill.getIssueDate();
        String prefix = "M" + issueDate.getYear();
        int next = bills.findTopByBillNumberStartingWithOrderByBillNumberDesc(prefix)
                .map(MonthlyRentBill::getBillNumber)
                .map(number -> number.substring(prefix.length()))
                .map(this::parseInt)
                .filter(value -> value != null)
                .map(value -> value + 1)
                .orElse(1);
        String billNumber;
        do {
            billNumber = prefix + String.format("%06d", next++);
        } while (bills.existsByBillNumber(billNumber));
        bill.setBillNumber(billNumber);
    }

    private void bindBill(MonthlyRentBill bill, Room room, Guest guest, YearMonth period, Map<String, String> params) {
        bill.setRoom(room);
        bill.setGuest(guest);
        bill.setBillingMonth(period.getMonthValue());
        bill.setBillingYear(period.getYear());
        bill.setRentAmount(parseMoney(params.get("rentAmount")));
        bill.setPreviousWaterMeter(parseMoney(params.get("previousWaterMeter")));
        bill.setCurrentWaterMeter(parseMoney(params.get("currentWaterMeter")));
        bill.setWaterRate(parseMoney(params.get("waterRate")));
        bill.setPreviousElectricMeter(parseMoney(params.get("previousElectricMeter")));
        bill.setCurrentElectricMeter(parseMoney(params.get("currentElectricMeter")));
        bill.setElectricRate(parseMoney(params.get("electricRate")));
        bill.setOtherAmount(parseMoney(params.get("otherAmount")));
        bill.setDiscountAmount(parseMoney(params.get("discountAmount")));
        bill.setAdvanceAppliedAmount(BigDecimal.ZERO);
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setIssueDate(parseDate(params.get("issueDate"), LocalDate.now()));
        bill.setDueDate(parseDate(params.get("dueDate"), LocalDate.now().plusDays(5)));
        bill.setNote(params.get("note"));
        bill.recalculate();
    }

    private void bindDefaultBill(MonthlyRentBill bill, Room room, Guest guest, YearMonth period) {
        MonthlyRentBill previousBill = bills.findPreviousBill(room, period.getMonthValue(), period.getYear()).stream().findFirst().orElse(null);
        bill.setRoom(room);
        bill.setGuest(guest);
        bill.setBillingMonth(period.getMonthValue());
        bill.setBillingYear(period.getYear());
        bill.setRentAmount(defaultRent(room, guest));
        bill.setPreviousWaterMeter(previousBill == null ? initialWaterMeter(guest) : previousBill.getCurrentWaterMeter());
        bill.setCurrentWaterMeter(previousBill == null ? initialWaterMeter(guest) : previousBill.getCurrentWaterMeter());
        bill.setPreviousElectricMeter(previousBill == null ? initialElectricMeter(guest) : previousBill.getCurrentElectricMeter());
        bill.setCurrentElectricMeter(previousBill == null ? initialElectricMeter(guest) : previousBill.getCurrentElectricMeter());
        bill.setWaterRate(settings.waterRate());
        bill.setElectricRate(settings.electricRate());
        bill.setIssueDate(LocalDate.now());
        bill.setDueDate(LocalDate.now().plusDays(5));
        bill.recalculate();
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

    private List<Integer> years() {
        int current = LocalDate.now().getYear();
        return java.util.stream.IntStream.rangeClosed(current, current + 3)
                .boxed()
                .toList();
    }

    private String thaiMonth(int month) {
        return List.of("มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน", "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม").get(month - 1);
    }

    private BigDecimal defaultRent(Room room, Guest guest) {
        if (guest.getPrice() != null && guest.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return guest.getPrice();
        }
        if (room.getRoomType() != null && room.getRoomType().getMonthlyPrice() != null) {
            return room.getRoomType().getMonthlyPrice();
        }
        return room.getMonthlyPrice() == null ? BigDecimal.ZERO : room.getMonthlyPrice();
    }

    private BigDecimal initialWaterMeter(Guest guest) {
        return guest.getInitialWaterMeter() == null ? BigDecimal.ZERO : guest.getInitialWaterMeter();
    }

    private BigDecimal initialElectricMeter(Guest guest) {
        return guest.getInitialElectricMeter() == null ? BigDecimal.ZERO : guest.getInitialElectricMeter();
    }

    private String redirectToBilling(Map<String, String> params, YearMonth period) {
        StringBuilder url = new StringBuilder("redirect:/finance/monthly-rent/billing?month=")
                .append(period.getMonthValue())
                .append("&year=")
                .append(period.getYear());
        append(url, "floorId", params.get("floorId"));
        append(url, "status", params.get("status"));
        append(url, "billNo", params.get("billNo"));
        append(url, "roomNo", params.get("roomNo"));
        append(url, "guestName", params.get("guestName"));
        return url.toString();
    }

    private void append(StringBuilder url, String name, String value) {
        if (value != null && !value.isBlank()) {
            url.append("&").append(name).append("=").append(value.replace(" ", "+"));
        }
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        try {
            return value == null || value.isBlank() ? fallback : LocalDate.parse(value);
        } catch (Exception ex) {
            return fallback;
        }
    }

    public record MonthOption(Integer value, String label) {
    }

    public class BillingRoomItem {
        private final Room room;
        private final Guest guest;
        private final MonthlyRentBill bill;
        private final MonthlyRentBill previousBill;
        private final YearMonth period;

        BillingRoomItem(Room room, Guest guest, MonthlyRentBill bill, MonthlyRentBill previousBill, YearMonth period) {
            this.room = room;
            this.guest = guest;
            this.bill = bill;
            this.previousBill = previousBill;
            this.period = period;
        }

        public Room getRoom() { return room; }
        public Guest getGuest() { return guest; }
        public MonthlyRentBill getBill() { return bill; }
        public MonthlyRentBill getPreviousBill() { return previousBill; }
        public YearMonth getPeriod() { return period; }
        public BigDecimal getDefaultRent() { return defaultRent(room, guest); }
        public BigDecimal getDefaultPreviousWaterMeter() { return previousBill == null ? initialWaterMeter(guest) : previousBill.getCurrentWaterMeter(); }
        public BigDecimal getDefaultPreviousElectricMeter() { return previousBill == null ? initialElectricMeter(guest) : previousBill.getCurrentElectricMeter(); }
        public BigDecimal getDefaultWaterRate() { return settings.waterRate(); }
        public BigDecimal getDefaultElectricRate() { return settings.electricRate(); }

        public MonthlyRentBillStatus getEffectiveStatus() {
            if (bill == null) return null;
            if (ISSUED_STATUSES.contains(bill.getStatus()) && bill.getDueDate() != null && bill.getDueDate().isBefore(LocalDate.now()) && bill.getStatus() != MonthlyRentBillStatus.PAID) {
                return MonthlyRentBillStatus.OVERDUE;
            }
            return bill.getStatus();
        }

        public String getStatusLabel() {
            MonthlyRentBillStatus status = getEffectiveStatus();
            if (status == null || status == MonthlyRentBillStatus.DRAFT) return "ยังไม่ออกบิล";
            if (status == MonthlyRentBillStatus.OVERDUE) return status.getLabel();
            return bill.getBillStatus() == null ? status.getLabel() : bill.getBillStatus().getName();
        }

        public String getStatusClass() {
            MonthlyRentBillStatus status = getEffectiveStatus();
            if (status == null) return "bill-unbilled";
            return switch (status) {
                case DRAFT -> "bill-draft";
                case PENDING -> "bill-pending";
                case PARTIAL_PAID -> "bill-partial";
                case PAID -> "bill-paid";
                case OVERDUE -> "bill-overdue";
                case CANCELLED -> "bill-cancelled";
            };
        }
    }
}
