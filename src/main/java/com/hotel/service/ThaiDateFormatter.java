package com.hotel.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component("thaiDate")
public class ThaiDateFormatter {
    private static final String[] THAI_MONTHS = {
            "มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
            "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"
    };

    public String format(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear() + 543);
    }

    public String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return format(dateTime.toLocalDate());
    }

    public String formatLong(LocalDate date) {
        if (date == null) {
            return "-";
        }
        return date.getDayOfMonth() + " " + THAI_MONTHS[date.getMonthValue() - 1] + " " + (date.getYear() + 543);
    }

    public String monthYear(Integer month, Integer year) {
        if (month == null || year == null || month < 1 || month > 12) {
            return "-";
        }
        return THAI_MONTHS[month - 1] + " " + (year + 543);
    }
}
