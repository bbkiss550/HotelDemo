package com.hotel.service;

import com.hotel.model.Reciept;
import com.hotel.model.RecieptType;
import com.hotel.model.Payment;
import com.hotel.repository.RecieptRepository;
import com.hotel.repository.RecieptTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecieptRecordService {
    private final RecieptRepository reciepts;
    private final RecieptTypeRepository types;

    public RecieptRecordService(RecieptRepository reciepts, RecieptTypeRepository types) {
        this.reciepts = reciepts;
        this.types = types;
    }

    @Transactional
    public Reciept record(long typeId, BigDecimal amount) {
        return record(typeId, amount, null);
    }

    @Transactional
    public synchronized Reciept record(long typeId, BigDecimal amount, Payment payment) {
        amount = amount == null ? BigDecimal.ZERO : amount;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (payment != null && payment.getReciept() != null) {
            return payment.getReciept();
        }
        Reciept reciept = new Reciept();
        reciept.setRecieptDate(LocalDate.now());
        reciept.setRecieptNo(nextRecieptNo(LocalDate.now()));
        reciept.setType(types.findById(typeId).orElseThrow());
        reciept.setAmount(amount);
        Reciept saved = reciepts.save(reciept);
        if (payment != null) {
            payment.setReciept(saved);
        }
        return saved;
    }

    public Reciept recordOpeningMonthly(BigDecimal amount) {
        return record(RecieptType.OPENING_MONTHLY, amount);
    }

    public Reciept recordOpeningMonthly(Payment payment, BigDecimal amount) {
        return record(RecieptType.OPENING_MONTHLY, amount, payment);
    }

    public Reciept recordDailyService(BigDecimal amount) {
        return record(RecieptType.DAILY_SERVICE, amount);
    }

    public Reciept recordDailyService(Payment payment, BigDecimal amount) {
        return record(RecieptType.DAILY_SERVICE, amount, payment);
    }

    public Reciept recordMonthlyRent(BigDecimal amount) {
        return record(RecieptType.MONTHLY_RENT, amount);
    }

    public Reciept recordMonthlyRent(Payment payment) {
        return record(RecieptType.MONTHLY_RENT, payment == null ? BigDecimal.ZERO : payment.getTotalAmount(), payment);
    }

    public Reciept recordBookingDeposit(Payment payment) {
        return record(RecieptType.BOOKING_DEPOSIT, payment == null ? BigDecimal.ZERO : payment.getTotalAmount(), payment);
    }

    private String nextRecieptNo(LocalDate date) {
        LocalDate recieptDate = date == null ? LocalDate.now() : date;
        String prefix = "P" + recieptDate.getYear();
        int nextNumber = reciepts.findTopByRecieptNoStartingWithOrderByRecieptNoDesc(prefix)
                .map(Reciept::getRecieptNo)
                .map(recieptNo -> recieptNo.substring(prefix.length()))
                .filter(suffix -> suffix.matches("\\d+"))
                .map(Integer::parseInt)
                .orElse(0) + 1;
        return prefix + String.format("%06d", nextNumber);
    }
}
