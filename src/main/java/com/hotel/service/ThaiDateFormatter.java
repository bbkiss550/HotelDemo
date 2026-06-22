package com.hotel.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component("thaiDate")
public class ThaiDateFormatter {
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
}
