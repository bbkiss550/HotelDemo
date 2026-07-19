package com.hotel.service;

import com.hotel.model.DailyCheckoutPenaltyChargeType;
import com.hotel.model.DailyCheckoutPenaltyRule;
import com.hotel.model.Guest;
import com.hotel.model.StayType;
import com.hotel.repository.DailyCheckoutPenaltyRuleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyCheckoutPenaltyService {
    private final DailyCheckoutPenaltyRuleRepository rules;
    private final AppSettingService settings;

    public DailyCheckoutPenaltyService(DailyCheckoutPenaltyRuleRepository rules, AppSettingService settings) {
        this.rules = rules;
        this.settings = settings;
    }

    public List<DailyCheckoutPenaltyRule> rules() {
        return rules.findAllByOrderBySortOrderAscIdAsc();
    }

    public PenaltyQuote quote(Guest guest) {
        if (guest == null || guest.getStayType() != StayType.DAILY || guest.getCheckOutDate() == null) {
            return PenaltyQuote.none();
        }
        LocalDateTime actualCheckout = LocalDateTime.now();
        LocalDateTime scheduledCheckout = LocalDateTime.of(guest.getCheckOutDate(), settings.dailyCheckoutTime());
        if (!actualCheckout.isAfter(scheduledCheckout)) {
            return PenaltyQuote.none(scheduledCheckout);
        }
        Duration overdue = Duration.between(scheduledCheckout, actualCheckout);
        long fullDayCount = overdue.toDays();
        LocalDateTime partialStart = scheduledCheckout.plusDays(fullDayCount);
        long partialMinutes = overdueMinutes(partialStart, actualCheckout);
        BigDecimal fullDayAmount = money(guest.getPrice()).multiply(BigDecimal.valueOf(fullDayCount));
        PartialPenalty partialPenalty = partialMinutes == 0
                ? PartialPenalty.none()
                : partialPenalty(guest, actualCheckout);

        return new PenaltyQuote(
                fullDayAmount.add(partialPenalty.amount()),
                partialPenalty.ruleId(),
                partialPenalty.description(),
                scheduledCheckout,
                fullDayCount,
                partialMinutes,
                fullDayAmount,
                partialPenalty.amount());
    }

    @Transactional
    public void saveRules(List<Long> ids,
                          List<String> startTimes,
                          List<String> endTimes,
                          List<String> chargeTypes,
                          List<BigDecimal> chargeValues,
                          List<String> enabledValues) {
        Map<Long, DailyCheckoutPenaltyRule> existing = new HashMap<>();
        rules.findAllByOrderBySortOrderAscIdAsc().forEach(rule -> existing.put(rule.getId(), rule));
        Set<Long> retainedIds = new HashSet<>();
        List<DailyCheckoutPenaltyRule> savedRules = new ArrayList<>();
        int ruleCount = startTimes == null ? 0 : startTimes.size();

        for (int index = 0; index < ruleCount; index++) {
            LocalTime startTime = parseTime(valueAt(startTimes, index));
            if (startTime == null) {
                continue;
            }
            Long id = valueAt(ids, index);
            DailyCheckoutPenaltyRule rule = id == null ? new DailyCheckoutPenaltyRule() : existing.getOrDefault(id, new DailyCheckoutPenaltyRule());
            LocalTime endTime = parseTime(valueAt(endTimes, index));
            if (endTime != null && !endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("เวลาสิ้นสุดของกฎต้องอยู่หลังเวลาเริ่มต้น");
            }

            rule.setStartDayOffset(0);
            rule.setStartTime(startTime);
            rule.setEndDayOffset(endTime == null ? null : 0);
            rule.setEndTime(endTime);
            rule.setChargeType(parseChargeType(valueAt(chargeTypes, index)));
            rule.setChargeValue(money(valueAt(chargeValues, index)));
            rule.setEnabled(enabledValues != null && enabledValues.contains(String.valueOf(index)));
            rule.setSortOrder(savedRules.size() + 1);
            savedRules.add(rule);
        }

        for (DailyCheckoutPenaltyRule rule : rules.saveAll(savedRules)) {
            if (rule.getId() != null) {
                retainedIds.add(rule.getId());
            }
        }
        existing.keySet().stream()
                .filter(id -> !retainedIds.contains(id))
                .forEach(rules::deleteById);
    }

    private BigDecimal amountFor(DailyCheckoutPenaltyRule rule, Guest guest, LocalDateTime startsAt, LocalDateTime actualCheckout) {
        BigDecimal value = money(rule.getChargeValue());
        BigDecimal dailyRate = money(guest.getPrice());
        DailyCheckoutPenaltyChargeType type = rule.getChargeType() == null
                ? DailyCheckoutPenaltyChargeType.FREE
                : rule.getChargeType();
        return switch (type) {
            case FREE -> BigDecimal.ZERO;
            case FIXED -> value;
            case PERCENT_DAILY_RATE -> dailyRate.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case ADD_NIGHTS -> dailyRate.multiply(value);
            case PER_HOUR -> value.multiply(BigDecimal.valueOf(hoursFrom(startsAt, actualCheckout)));
        };
    }

    private long hoursFrom(LocalDateTime startsAt, LocalDateTime actualCheckout) {
        long minutes = Math.max(0, Duration.between(startsAt, actualCheckout).toMinutes());
        return Math.max(1, (minutes + 59) / 60);
    }

    private long overdueMinutes(LocalDateTime scheduledCheckout, LocalDateTime actualCheckout) {
        long seconds = Math.max(0, Duration.between(scheduledCheckout, actualCheckout).getSeconds());
        return seconds == 0 ? 0 : (seconds + 59) / 60;
    }

    private PartialPenalty partialPenalty(Guest guest, LocalDateTime actualCheckout) {
        for (DailyCheckoutPenaltyRule rule : rules()) {
            if (!Boolean.TRUE.equals(rule.getEnabled()) || rule.getStartTime() == null) {
                continue;
            }
            LocalDateTime startsAt = actualCheckout.toLocalDate().atTime(rule.getStartTime());
            LocalDateTime endsAt = rule.getEndTime() == null
                    ? actualCheckout.toLocalDate().atTime(LocalTime.MAX)
                    : actualCheckout.toLocalDate().atTime(rule.getEndTime());
            if (actualCheckout.isBefore(startsAt) || !actualCheckout.isBefore(endsAt)) {
                continue;
            }
            return new PartialPenalty(amountFor(rule, guest, startsAt, actualCheckout), rule.getId(), describe(rule));
        }
        return PartialPenalty.none();
    }

    private String describe(DailyCheckoutPenaltyRule rule) {
        String range = rule.getStartTime() + (rule.getEndTime() == null ? "+" : " - " + rule.getEndTime());
        return rule.getChargeType().getLabel() + " (" + range + ")";
    }

    private DailyCheckoutPenaltyChargeType parseChargeType(String value) {
        try {
            return DailyCheckoutPenaltyChargeType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return DailyCheckoutPenaltyChargeType.FIXED;
        }
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("รูปแบบเวลาในกฎค่าปรับไม่ถูกต้อง");
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }

    private <T> T valueAt(List<T> values, int index) {
        return values != null && index >= 0 && index < values.size() ? values.get(index) : null;
    }

    public record PenaltyQuote(BigDecimal amount,
                               Long ruleId,
                               String description,
                               LocalDateTime scheduledCheckout,
                               long fullDayCount,
                               long betweenDayMinutes,
                               BigDecimal fullDayAmount,
                               BigDecimal betweenDayAmount) {
        public PenaltyQuote(BigDecimal amount, Long ruleId, String description) {
            this(amount, ruleId, description, null, 0, 0, BigDecimal.ZERO, amount == null ? BigDecimal.ZERO : amount);
        }

        public long overdueDays() {
            return fullDayCount;
        }

        public long overdueHours() {
            return betweenDayMinutes / 60;
        }

        public long overdueRemainingMinutes() {
            return betweenDayMinutes % 60;
        }

        public static PenaltyQuote none() {
            return none(null);
        }

        public static PenaltyQuote none(LocalDateTime scheduledCheckout) {
            return new PenaltyQuote(BigDecimal.ZERO, null, "ยังไม่เกินเวลาเช็คเอาต์", scheduledCheckout, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private record PartialPenalty(BigDecimal amount, Long ruleId, String description) {
        private static PartialPenalty none() {
            return new PartialPenalty(BigDecimal.ZERO, null, "ไม่พบกฎสำหรับช่วงเวลาระหว่างวัน");
        }
    }
}
