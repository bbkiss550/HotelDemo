package com.hotel.service;

import com.hotel.model.MonthlyRentBillStatus;

import com.hotel.model.LookupCodes;

import com.hotel.model.AdvanceLedger;
import com.hotel.model.Guest;
import com.hotel.model.MonthlyRentBill;
import com.hotel.model.Payment;
import com.hotel.repository.AdvanceLedgerRepository;
import com.hotel.repository.GuestRepository;
import java.math.BigDecimal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdvanceBalanceService {
    private final GuestRepository guests;
    private final AdvanceLedgerRepository ledgers;

    public AdvanceBalanceService(GuestRepository guests, AdvanceLedgerRepository ledgers) {
        this.guests = guests;
        this.ledgers = ledgers;
    }

    @Transactional
    public void addAdvanceFromPayment(Payment payment) {
        Guest guest = payment.getGuest();
        BigDecimal amount = money(payment.getAmount());
        if (guest == null || guest.getId() == null) {
            throw new IllegalArgumentException("ต้องเลือกผู้เช่ารายเดือนที่ active");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("จำนวนเงินต้องมากกว่า 0");
        }
        guest = guests.findById(guest.getId()).orElseThrow();
        if (!Boolean.TRUE.equals(guest.getActive()) || !LookupCodes.MONTHLY.equals(guest.getStayType())) {
            throw new IllegalArgumentException("ต้องเลือกผู้เช่ารายเดือนที่ active");
        }
        BigDecimal before = money(guest.getAdvanceBalance());
        BigDecimal after = before.add(amount);
        guest.setAdvanceBalance(after);
        guests.save(guest);
        saveLedger(guest, payment.getRoom(), null, payment, LookupCodes.ADD_ADVANCE, amount, before, after, payment.getRemark());
    }

    @Transactional
    public void addOpeningAdvance(Guest guest, BigDecimal amount) {
        amount = money(amount);
        if (guest == null || guest.getId() == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        guest = guests.findById(guest.getId()).orElseThrow();
        BigDecimal before = money(guest.getAdvanceBalance());
        BigDecimal after = before.add(amount);
        guest.setAdvanceBalance(after);
        guests.save(guest);
        saveLedger(guest, guest.getRoom(), null, null, LookupCodes.ADD_ADVANCE, amount, before, after,
                "รับค่าเช่าล่วงหน้าวันเข้าพัก " + amount.toPlainString());
    }

    @Transactional
    public void applyAdvanceToIssuedBill(MonthlyRentBill bill) {
        bill.recalculate();
        if (bill.getId() != null && ledgers.existsByBillAndType(bill, LookupCodes.APPLY_ADVANCE)) {
            return;
        }
        Guest guest = guests.findById(bill.getGuest().getId()).orElseThrow();
        BigDecimal before = money(guest.getAdvanceBalance());
        BigDecimal eligibleAmount = money(bill.getRentAmount()).min(money(bill.getSubtotalAmount())).max(BigDecimal.ZERO);
        BigDecimal applied = before.min(eligibleAmount).max(BigDecimal.ZERO);
        bill.setAdvanceAppliedAmount(applied);
        bill.recalculate();
        BigDecimal after = before.subtract(applied).max(BigDecimal.ZERO);
        if (applied.compareTo(BigDecimal.ZERO) > 0) {
            guest.setAdvanceBalance(after);
            guests.save(guest);
            saveLedger(guest, bill.getRoom(), bill, null, LookupCodes.APPLY_ADVANCE, applied.negate(), before, after,
                    "หักเครดิตล่วงหน้าบิล " + bill.getBillingMonth() + "/" + (bill.getBillingYear() + 543));
        }
        if (bill.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(MonthlyRentBillStatus.PAID);
        } else if (applied.compareTo(BigDecimal.ZERO) > 0) {
            bill.setStatus(MonthlyRentBillStatus.PARTIAL_PAID);
        } else {
            bill.setStatus(MonthlyRentBillStatus.PENDING);
        }
    }

    @Transactional
    public void restoreAdvanceFromCancelledBill(MonthlyRentBill bill) {
        BigDecimal applied = money(bill.getAdvanceAppliedAmount());
        if (bill.getGuest() == null || bill.getGuest().getId() == null || applied.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Guest guest = guests.findById(bill.getGuest().getId()).orElseThrow();
        BigDecimal before = money(guest.getAdvanceBalance());
        BigDecimal after = before.add(applied);
        guest.setAdvanceBalance(after);
        guests.save(guest);
        saveLedger(guest, bill.getRoom(), bill, null, LookupCodes.ADJUST_ADVANCE, applied, before, after,
                "คืนเครดิตจากการยกเลิกบิล " + bill.getBillingMonth() + "/" + (bill.getBillingYear() + 543));
        bill.setAdvanceAppliedAmount(BigDecimal.ZERO);
        bill.recalculate();
    }

    private void saveLedger(Guest guest, com.hotel.model.Room room, MonthlyRentBill bill, Payment payment,
                            String type, BigDecimal amount, BigDecimal before, BigDecimal after, String note) {
        AdvanceLedger ledger = new AdvanceLedger();
        ledger.setGuest(guest);
        ledger.setRoom(room != null ? room : guest.getRoom());
        ledger.setBill(bill);
        ledger.setPayment(payment);
        ledger.setType(type);
        ledger.setAmount(amount);
        ledger.setBalanceBefore(before);
        ledger.setBalanceAfter(after);
        ledger.setNote(note);
        ledger.setCreatedBy(currentUsername());
        ledgers.save(ledger);
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
