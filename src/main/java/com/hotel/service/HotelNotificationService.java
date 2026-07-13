package com.hotel.service;

import com.hotel.model.Guest;
import com.hotel.model.MonthlyRentBill;
import com.hotel.model.MonthlyRentBillStatus;
import com.hotel.model.StayType;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.MonthlyRentBillRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HotelNotificationService {
    private static final List<Long> UNPAID_BILL_STATUS_IDS = List.of(
            MonthlyRentBillStatus.PENDING.getId(),
            MonthlyRentBillStatus.PARTIAL_PAID.getId());

    private final GuestRepository guests;
    private final MonthlyRentBillRepository monthlyBills;

    public HotelNotificationService(GuestRepository guests, MonthlyRentBillRepository monthlyBills) {
        this.guests = guests;
        this.monthlyBills = monthlyBills;
    }

    public NotificationSummary summary() {
        LocalDate today = LocalDate.now();
        long overdueDailyStayCount = guests.countByActiveTrueAndStayTypeAndCheckOutDateBefore(StayType.DAILY, today);
        long overdueBillCount = monthlyBills.countByStatusIdInAndDueDateBefore(UNPAID_BILL_STATUS_IDS, today);
        return new NotificationSummary(
                overdueDailyStayCount,
                overdueBillCount,
                guests.findTop5ByActiveTrueAndStayTypeAndCheckOutDateBeforeOrderByCheckOutDateAscIdAsc(StayType.DAILY, today),
                monthlyBills.findTop5ByStatusIdInAndDueDateBeforeOrderByDueDateAscIdAsc(UNPAID_BILL_STATUS_IDS, today));
    }

    public record NotificationSummary(long overdueDailyStayCount,
                                      long overdueBillCount,
                                      List<Guest> overdueDailyGuests,
                                      List<MonthlyRentBill> overdueBills) {
        public long getTotalCount() {
            return overdueDailyStayCount + overdueBillCount;
        }
    }
}
