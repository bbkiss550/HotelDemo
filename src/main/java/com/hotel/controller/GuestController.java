package com.hotel.controller;

import com.hotel.model.Guest;
import com.hotel.model.Booking;
import com.hotel.model.BookingStatus;
import com.hotel.model.DepositRefund;
import com.hotel.model.DepositRefundItem;
import com.hotel.model.Payment;
import com.hotel.model.PaymentStatus;
import com.hotel.model.Room;
import com.hotel.model.RoomStatus;
import com.hotel.model.RoomTransfer;
import com.hotel.model.StayType;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.AdvanceLedgerRepository;
import com.hotel.repository.DepositRefundRepository;
import com.hotel.repository.FloorRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.PaymentRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.repository.RoomTransferRepository;
import com.hotel.repository.RoomTypeRepository;
import com.hotel.service.AdvanceBalanceService;
import com.hotel.service.AppSettingService;
import com.hotel.service.AuditService;
import com.hotel.service.DailyCheckoutPenaltyService;
import com.hotel.service.RecieptRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/guests")
public class GuestController {
    private final GuestRepository guests;
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final RoomTypeRepository roomTypes;
    private final FloorRepository floors;
    private final PaymentRepository payments;
    private final AdvanceLedgerRepository advanceLedgers;
    private final AdvanceBalanceService advanceBalanceService;
    private final AppSettingService settings;
    private final RecieptRecordService recieptRecordService;
    private final DepositRefundRepository depositRefunds;
    private final RoomTransferRepository roomTransfers;
    private final DailyCheckoutPenaltyService checkoutPenalties;
    private final AuditService audit;

    public GuestController(GuestRepository guests, BookingRepository bookings, RoomRepository rooms, RoomTypeRepository roomTypes, FloorRepository floors, PaymentRepository payments, AdvanceLedgerRepository advanceLedgers, AdvanceBalanceService advanceBalanceService, AppSettingService settings, RecieptRecordService recieptRecordService, DepositRefundRepository depositRefunds, RoomTransferRepository roomTransfers, DailyCheckoutPenaltyService checkoutPenalties, AuditService audit) {
        this.guests = guests;
        this.bookings = bookings;
        this.rooms = rooms;
        this.roomTypes = roomTypes;
        this.floors = floors;
        this.payments = payments;
        this.advanceLedgers = advanceLedgers;
        this.advanceBalanceService = advanceBalanceService;
        this.settings = settings;
        this.recieptRecordService = recieptRecordService;
        this.depositRefunds = depositRefunds;
        this.roomTransfers = roomTransfers;
        this.checkoutPenalties = checkoutPenalties;
        this.audit = audit;
    }

    @GetMapping
    String index(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) Long floorId, Model model) {
        var floorList = floors.findAllByOrderBySortOrderAscNumberAscNameAsc();
        var selectedFloor = floorId == null
                ? floorList.stream().findFirst().orElse(null)
                : floors.findById(floorId).orElse(floorList.stream().findFirst().orElse(null));

        var roomList = q.isBlank()
                ? rooms.findAllByOrderByRoomNumber()
                : rooms.findByRoomNumberContainingIgnoreCaseOrderByRoomNumber(q);
        Map<Long, Guest> activeGuests = new HashMap<>();
        Map<Long, List<Payment>> activeGuestPayments = new HashMap<>();
        Map<Long, Long> activeGuestStayDays = new HashMap<>();
        Map<Long, DailyCheckoutPenaltyService.PenaltyQuote> activeGuestCheckoutPenaltyQuotes = new HashMap<>();
        Map<Long, Booking> reservedBookings = new HashMap<>();
        for (var room : roomList) {
            guests.findTopByRoomAndActiveTrueOrderByCheckInDateDescIdDesc(room)
                    .ifPresent(guest -> {
                        activeGuests.put(room.getId(), guest);
                        activeGuestPayments.put(guest.getId(), payments.findByGuestIdOrderByPaymentDateDescIdDesc(guest.getId()));
                        if (guest.getStayType() == StayType.DAILY) {
                            LocalDate checkIn = guest.getCheckInDate() == null ? LocalDate.now() : guest.getCheckInDate();
                            LocalDate checkOut = guest.getCheckOutDate() == null ? checkIn.plusDays(1) : guest.getCheckOutDate();
                            activeGuestStayDays.put(guest.getId(), Math.max(1, ChronoUnit.DAYS.between(checkIn, checkOut)));
                        }
                        activeGuestCheckoutPenaltyQuotes.put(guest.getId(), checkoutPenalties.quote(guest));
                    });
            if (room.getStatus() == RoomStatus.RESERVED) {
                bookings.findTopByRoomAndStatusOrderByCheckInDateDescIdDesc(room, BookingStatus.CONFIRMED)
                        .ifPresent(booking -> reservedBookings.put(room.getId(), booking));
            }
        }

        model.addAttribute("q", q);
        model.addAttribute("floors", floorList);
        model.addAttribute("selectedFloor", selectedFloor);
        model.addAttribute("rooms", roomList);
        model.addAttribute("activeGuests", activeGuests);
        model.addAttribute("activeGuestPayments", activeGuestPayments);
        model.addAttribute("activeGuestStayDays", activeGuestStayDays);
        model.addAttribute("activeGuestCheckoutPenaltyQuotes", activeGuestCheckoutPenaltyQuotes);
        model.addAttribute("reservedBookings", reservedBookings);
        model.addAttribute("unassignedBookings", bookings.findByRoomIsNullAndStatusOrderByCheckInDateAscIdDesc(BookingStatus.CONFIRMED));
        model.addAttribute("availableRooms", rooms.findByStatusOrderByRoomNumber(RoomStatus.AVAILABLE));
        model.addAttribute("roomTypes", roomTypes.findAllByOrderByNameAsc());
        model.addAttribute("stayTypes", StayType.values());
        model.addAttribute("defaultDepositAmount", settings.defaultDeposit());
        model.addAttribute("monthlyDepositAmount", settings.monthlyDeposit());
        return "guests/index";
    }

    @PostMapping
    @Transactional
    String save(@ModelAttribute Guest guest,
                @RequestParam Long roomId,
                @RequestParam(required = false) Long bookingId,
                @RequestParam(required = false) BigDecimal bookingDepositAmount,
                @RequestParam(required = false) Long floorId,
                RedirectAttributes redirect) {
        var room = rooms.findById(roomId).orElseThrow();
        if (room.getStatus() != RoomStatus.AVAILABLE && room.getStatus() != RoomStatus.RESERVED && bookingId != null) {
            redirect.addFlashAttribute("error", "ห้องที่เลือกไม่ว่าง");
            redirect.addFlashAttribute("flashType", "warning");
            return floorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + floorId;
        }
        guest.setRoom(room);
        guest.setActive(true);
        if (guest.getInitialWaterMeter() == null) {
            guest.setInitialWaterMeter(BigDecimal.ZERO);
        }
        if (guest.getInitialElectricMeter() == null) {
            guest.setInitialElectricMeter(BigDecimal.ZERO);
        }
        if (bookingId != null) {
            var booking = bookings.findById(bookingId).orElseThrow();
            if (booking.getStayType() != guest.getStayType()) {
                redirect.addFlashAttribute("error", "รูปแบบการพักต้องตรงกับการจอง");
                redirect.addFlashAttribute("flashType", "warning");
                return floorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + floorId;
            }
            if (booking.getRoomType() != null && (room.getRoomType() == null || !booking.getRoomType().getId().equals(room.getRoomType().getId()))) {
                redirect.addFlashAttribute("error", "ห้องที่เลือกไม่ตรงกับประเภทห้องที่จอง");
                redirect.addFlashAttribute("flashType", "warning");
                return floorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + floorId;
            }
        }
        var bookingDeposit = bookingDepositAmount == null ? BigDecimal.ZERO : bookingDepositAmount;
        calculateGuestAmounts(guest, bookingDeposit);
        if (guest.getCheckInDate() == null) {
            guest.setCheckInDate(LocalDate.now());
        }
        if (guest.getTotalPaid() == null || guest.getTotalPaid().compareTo(bookingDeposit) < 0) {
            guest.setTotalPaid(bookingDeposit);
        }
        guests.save(guest);
        createOpeningPayment(guest);
        if (guest.getStayType() == StayType.MONTHLY) {
            advanceBalanceService.addOpeningAdvance(guest, monthlyAdvanceAmount(guest));
        }
        if (bookingId != null) {
            var booking = bookings.findById(bookingId).orElseThrow();
            booking.setRoom(room);
            booking.setStatus(BookingStatus.CHECKED_IN);
            bookings.save(booking);
        }
        room.setStatus(guest.getStayType() == StayType.MONTHLY ? RoomStatus.MONTHLY_OCCUPIED : RoomStatus.DAILY_OCCUPIED);
        rooms.save(room);
        audit.record("CHECK_IN", "Room " + room.getRoomNumber() + " guest " + guest.getFullName());
        redirect.addFlashAttribute("message", "บันทึกเข้าพักห้อง " + room.getRoomNumber() + " เรียบร้อย");
        redirect.addFlashAttribute("flashType", "success");
        return floorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + floorId;
    }

    @PostMapping("/{id}/checkout")
    @Transactional
    String checkout(@PathVariable Long id,
                    @RequestParam(required = false) Long floorId,
                    @RequestParam(required = false) List<String> deductName,
                    @RequestParam(required = false) List<String> deductAmount,
                    @RequestParam(defaultValue = "เงินสด") String refundMethod,
                    @RequestParam(required = false) String refundRemark,
                    @RequestParam(defaultValue = "false") boolean applyCheckoutPenalty,
                    @RequestParam(defaultValue = "RULE") String checkoutPenaltyMode,
                    @RequestParam(defaultValue = "0") BigDecimal checkoutPenaltyAmount,
                    RedirectAttributes redirect) {
        var guest = guests.findById(id).orElseThrow();
        DailyCheckoutPenaltyService.PenaltyQuote checkoutPenalty = checkoutPenalty(guest, applyCheckoutPenalty, checkoutPenaltyMode, checkoutPenaltyAmount);
        recordDepositRefund(guest, deductName, deductAmount, refundMethod, refundRemark, checkoutPenalty);
        guest.setActive(false);
        if (guest.getCheckOutDate() == null) {
            guest.setCheckOutDate(LocalDate.now());
        }
        guests.save(guest);
        var room = guest.getRoom();
        room.setStatus(RoomStatus.AVAILABLE);
        rooms.save(room);
        audit.record("CHECK_OUT", "Room " + room.getRoomNumber() + " guest " + guest.getFullName());
        redirect.addFlashAttribute("message", "เช็กเอาต์ห้อง " + room.getRoomNumber() + " เรียบร้อย");
        redirect.addFlashAttribute("flashType", "delete");
        return floorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + floorId;
    }

    @PostMapping("/{id}/transfer-room")
    @Transactional
    String transferRoom(@PathVariable Long id,
                        @RequestParam Long toRoomId,
                        @RequestParam(required = false) Long floorId,
                        @RequestParam(required = false) String remark,
                        RedirectAttributes redirect) {
        var guest = guests.findById(id).orElseThrow();
        var fromRoom = guest.getRoom();
        var toRoom = rooms.findById(toRoomId).orElseThrow();
        if (!Boolean.TRUE.equals(guest.getActive()) || fromRoom == null) {
            redirect.addFlashAttribute("error", "ไม่พบข้อมูลผู้เข้าพักที่ยังใช้งานอยู่");
            redirect.addFlashAttribute("flashType", "warning");
            return floorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + floorId;
        }
        if (fromRoom.getId().equals(toRoom.getId())) {
            redirect.addFlashAttribute("error", "ห้องปลายทางต้องไม่ใช่ห้องเดิม");
            redirect.addFlashAttribute("flashType", "warning");
            return floorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + floorId;
        }
        if (toRoom.getStatus() != RoomStatus.AVAILABLE) {
            redirect.addFlashAttribute("error", "ย้ายได้เฉพาะไปยังห้องว่างเท่านั้น");
            redirect.addFlashAttribute("flashType", "warning");
            return floorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + floorId;
        }

        RoomTransfer transfer = new RoomTransfer();
        transfer.setGuest(guest);
        transfer.setFromRoom(fromRoom);
        transfer.setToRoom(toRoom);
        transfer.setTransferDate(LocalDate.now());
        transfer.setStayType(guest.getStayType());
        transfer.setOldPrice(roomPriceForStay(fromRoom, guest.getStayType()));
        transfer.setNewPrice(roomPriceForStay(toRoom, guest.getStayType()));
        transfer.setRemark(remark);

        fromRoom.setStatus(RoomStatus.AVAILABLE);
        toRoom.setStatus(guest.getStayType() == StayType.MONTHLY ? RoomStatus.MONTHLY_OCCUPIED : RoomStatus.DAILY_OCCUPIED);
        guest.setRoom(toRoom);
        guest.setPrice(roomPriceForStay(toRoom, guest.getStayType()));

        rooms.save(fromRoom);
        rooms.save(toRoom);
        guests.save(guest);
        roomTransfers.save(transfer);

        audit.record("TRANSFER_ROOM", "Guest " + guest.getFullName() + " room " + fromRoom.getRoomNumber() + " to " + toRoom.getRoomNumber());
        redirect.addFlashAttribute("message", "ย้ายห้องจาก " + fromRoom.getRoomNumber() + " ไป " + toRoom.getRoomNumber() + " เรียบร้อย");
        redirect.addFlashAttribute("flashType", "success");
        Long targetFloorId = toRoom.getFloor() != null ? toRoom.getFloor().getId() : floorId;
        return targetFloorId == null ? "redirect:/guests" : "redirect:/guests?floorId=" + targetFloorId;
    }

    @GetMapping("/{id}")
    String detail(@PathVariable Long id, Model model) {
        var guest = guests.findById(id).orElseThrow();
        model.addAttribute("guest", guest);
        model.addAttribute("payments", payments.findByGuestIdOrderByPaymentDateDescIdDesc(id));
        model.addAttribute("advanceLedgers", advanceLedgers.findByGuestIdOrderByCreatedAtDescIdDesc(id));
        return "guests/detail";
    }

    private void calculateGuestAmounts(Guest guest, BigDecimal bookingDeposit) {
        bookingDeposit = bookingDeposit == null ? BigDecimal.ZERO : bookingDeposit;
        BigDecimal initialPayment;
        if (guest.getStayType() == StayType.DAILY) {
            guest.setAdvanceMonths(null);
            BigDecimal price = guest.getPrice() == null ? BigDecimal.ZERO : guest.getPrice();
            BigDecimal deposit = guest.getDeposit() == null ? BigDecimal.ZERO : guest.getDeposit();
            long days = 1;
            if (guest.getCheckInDate() != null && guest.getCheckOutDate() != null) {
                days = Math.max(1, ChronoUnit.DAYS.between(guest.getCheckInDate(), guest.getCheckOutDate()));
            }
            initialPayment = price.multiply(BigDecimal.valueOf(days)).add(deposit);
        } else {
            BigDecimal price = guest.getPrice() == null ? BigDecimal.ZERO : guest.getPrice();
            BigDecimal deposit = guest.getDeposit() == null ? BigDecimal.ZERO : guest.getDeposit();
            int months = guest.getAdvanceMonths() == null ? 1 : guest.getAdvanceMonths();
            initialPayment = price.multiply(BigDecimal.valueOf(months)).add(deposit);
        }
        guest.setInitialPayment(initialPayment.subtract(bookingDeposit).max(BigDecimal.ZERO));
        if (guest.getTotalPaid() == null) {
            guest.setTotalPaid(BigDecimal.ZERO);
        }
    }

    private BigDecimal monthlyAdvanceAmount(Guest guest) {
        BigDecimal price = guest.getPrice() == null ? BigDecimal.ZERO : guest.getPrice();
        int months = guest.getAdvanceMonths() == null ? 1 : guest.getAdvanceMonths();
        return price.multiply(BigDecimal.valueOf(months));
    }

    private BigDecimal roomPriceForStay(Room room, StayType stayType) {
        if (room == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal price;
        if (stayType == StayType.MONTHLY) {
            price = room.getRoomType() != null ? room.getRoomType().getMonthlyPrice() : room.getMonthlyPrice();
        } else {
            price = room.getRoomType() != null ? room.getRoomType().getNightlyPrice() : room.getNightlyPrice();
        }
        return price == null ? BigDecimal.ZERO : price;
    }

    private void recordDepositRefund(Guest guest, List<String> deductNames, List<String> deductAmounts, String refundMethod, String refundRemark, DailyCheckoutPenaltyService.PenaltyQuote checkoutPenalty) {
        BigDecimal deposit = guest.getDeposit() == null ? BigDecimal.ZERO : guest.getDeposit();
        deductNames = deductNames == null ? Collections.emptyList() : deductNames;
        deductAmounts = deductAmounts == null ? Collections.emptyList() : deductAmounts;

        DepositRefund refund = new DepositRefund();
        refund.setRefundNo(nextRefundNo(LocalDate.now()));
        refund.setRefundDate(LocalDate.now());
        refund.setGuest(guest);
        refund.setRoom(guest.getRoom());
        refund.setDepositAmount(deposit);
        refund.setRefundMethod(refundMethod == null || refundMethod.isBlank() ? "เงินสด" : refundMethod);
        refund.setRemark(refundRemark);

        BigDecimal totalDeduct = BigDecimal.ZERO;
        for (int index = 0; index < deductNames.size(); index++) {
            String name = deductNames.get(index);
            BigDecimal amount = index < deductAmounts.size() ? parseMoney(deductAmounts.get(index)) : BigDecimal.ZERO;
            if (name == null || name.isBlank() || amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            DepositRefundItem item = new DepositRefundItem();
            item.setItemName(name.trim());
            item.setItemAmount(amount);
            item.setSortOrder(index + 1);
            refund.addItem(item);
            totalDeduct = totalDeduct.add(amount);
        }

        if (checkoutPenalty.amount().compareTo(BigDecimal.ZERO) > 0) {
            DepositRefundItem item = new DepositRefundItem();
            item.setItemName("ค่าปรับเช็คเอาต์: " + checkoutPenalty.description());
            item.setItemAmount(checkoutPenalty.amount());
            item.setSortOrder(refund.getItems().size() + 1);
            refund.addItem(item);
            totalDeduct = totalDeduct.add(checkoutPenalty.amount());
        }

        refund.setTotalDeductAmount(totalDeduct);
        refund.setRefundAmount(deposit.subtract(totalDeduct).max(BigDecimal.ZERO));
        refund.setExtraChargeAmount(totalDeduct.subtract(deposit).max(BigDecimal.ZERO));
        if (deposit.compareTo(BigDecimal.ZERO) > 0 || totalDeduct.compareTo(BigDecimal.ZERO) > 0) {
            depositRefunds.save(refund);
        }
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private DailyCheckoutPenaltyService.PenaltyQuote checkoutPenalty(Guest guest,
                                                                      boolean applyCheckoutPenalty,
                                                                      String mode,
                                                                      BigDecimal manualAmount) {
        if (!applyCheckoutPenalty) {
            return DailyCheckoutPenaltyService.PenaltyQuote.none();
        }
        if ("MANUAL".equalsIgnoreCase(mode)) {
            BigDecimal amount = manualAmount == null ? BigDecimal.ZERO : manualAmount.max(BigDecimal.ZERO);
            return new DailyCheckoutPenaltyService.PenaltyQuote(amount, null, "กำหนดยอดเอง");
        }
        return checkoutPenalties.quote(guest);
    }

    private synchronized String nextRefundNo(LocalDate date) {
        LocalDate refundDate = date == null ? LocalDate.now() : date;
        String prefix = "R" + refundDate.getYear();
        int nextNumber = depositRefunds.findTopByRefundNoStartingWithOrderByRefundNoDesc(prefix)
                .map(DepositRefund::getRefundNo)
                .map(refundNo -> refundNo.substring(prefix.length()))
                .map(number -> {
                    try {
                        return Integer.parseInt(number);
                    } catch (NumberFormatException ex) {
                        return 0;
                    }
                })
                .orElse(0) + 1;
        return prefix + String.format("%06d", nextNumber);
    }

    private void createOpeningPayment(Guest guest) {
        BigDecimal amount = guest.getInitialPayment() == null ? BigDecimal.ZERO : guest.getInitialPayment();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Payment payment = new Payment();
        payment.setRoom(guest.getRoom());
        payment.setGuest(guest);
        payment.setAmount(amount);
        payment.setFineAmount(BigDecimal.ZERO);
        payment.setPaymentDate(guest.getCheckInDate() == null ? LocalDate.now() : guest.getCheckInDate());
        payment.setPaymentMethod("เงินสด");
        payment.setStatus(PaymentStatus.PAID);
        payment.setRemark("ค่าแรกเข้า");
        payment = payments.save(payment);
        if (guest.getStayType() == StayType.MONTHLY) {
            recieptRecordService.recordOpeningMonthly(payment, amount);
        } else {
            recieptRecordService.recordDailyService(payment, amount);
        }
        payments.save(payment);

        BigDecimal totalPaid = guest.getTotalPaid() == null ? BigDecimal.ZERO : guest.getTotalPaid();
        guest.setTotalPaid(totalPaid.add(amount));
        guests.save(guest);
    }
}
