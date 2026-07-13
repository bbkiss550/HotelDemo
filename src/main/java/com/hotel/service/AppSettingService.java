package com.hotel.service;

import com.hotel.model.AppSetting;
import com.hotel.repository.AppSettingRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppSettingService {
    public static final String DEFAULT_DEPOSIT = "default_deposit";
    public static final String MONTHLY_DEPOSIT = "monthly_deposit";
    public static final String ELECTRIC_RATE = "electric_rate";
    public static final String WATER_RATE = "water_rate";
    public static final String FINE_AMOUNT = "fine_amount";
    public static final String FINE_INTERVAL_DAYS = "fine_interval_days";
    public static final String CHECKOUT_OVERDUE_FINE_PER_HOUR = "checkout_overdue_fine_per_hour";
    public static final String DAILY_CHECKOUT_TIME = "daily_checkout_time";
    private static final String LEGACY_CHECKOUT_OVERDUE_FINE_PER_DAY = "checkout_overdue_fine_per_day";
    public static final String SYSTEM_NAME = "system_name";
    private static final String DEFAULT_SYSTEM_NAME = "BlueCatHotelDemo";
    private static final LocalTime DEFAULT_DAILY_CHECKOUT_TIME = LocalTime.NOON;

    private final AppSettingRepository settings;

    public AppSettingService(AppSettingRepository settings) {
        this.settings = settings;
    }

    public Map<String, BigDecimal> values() {
        Map<String, BigDecimal> values = defaults();
        settings.findAll().forEach(setting -> values.put(setting.getKey(), money(setting.getValue())));
        if (!settings.existsById(CHECKOUT_OVERDUE_FINE_PER_HOUR)) {
            values.put(CHECKOUT_OVERDUE_FINE_PER_HOUR, value(LEGACY_CHECKOUT_OVERDUE_FINE_PER_DAY));
        }
        return values;
    }

    public BigDecimal defaultDeposit() {
        return value(DEFAULT_DEPOSIT);
    }

    public BigDecimal monthlyDeposit() {
        return value(MONTHLY_DEPOSIT);
    }

    public BigDecimal electricRate() {
        return value(ELECTRIC_RATE);
    }

    public BigDecimal waterRate() {
        return value(WATER_RATE);
    }

    public BigDecimal fineAmount() {
        return value(FINE_AMOUNT);
    }

    public Integer fineIntervalDays() {
        return value(FINE_INTERVAL_DAYS).max(BigDecimal.ONE).intValue();
    }

    public BigDecimal checkoutOverdueFinePerHour() {
        return settings.findById(CHECKOUT_OVERDUE_FINE_PER_HOUR)
                .map(AppSetting::getValue)
                .map(this::money)
                .orElseGet(() -> value(LEGACY_CHECKOUT_OVERDUE_FINE_PER_DAY));
    }

    public LocalTime dailyCheckoutTime() {
        String value = settings.findById(DAILY_CHECKOUT_TIME)
                .map(AppSetting::getTextValue)
                .orElse(null);
        try {
            return value == null || value.isBlank() ? DEFAULT_DAILY_CHECKOUT_TIME : LocalTime.parse(value);
        } catch (java.time.format.DateTimeParseException ex) {
            return DEFAULT_DAILY_CHECKOUT_TIME;
        }
    }

    public String systemName() {
        return settings.findById(SYSTEM_NAME)
                .map(AppSetting::getTextValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(DEFAULT_SYSTEM_NAME);
    }

    @Transactional
    public void saveDefaults(String systemName, BigDecimal defaultDeposit, BigDecimal monthlyDeposit, BigDecimal electricRate, BigDecimal waterRate, BigDecimal fineAmount, Integer fineIntervalDays, BigDecimal checkoutOverdueFinePerHour, LocalTime dailyCheckoutTime) {
        saveText(SYSTEM_NAME, "ชื่อระบบ", systemName == null || systemName.isBlank() ? DEFAULT_SYSTEM_NAME : systemName.trim());
        save(DEFAULT_DEPOSIT, "ค่าประกันรายวัน", defaultDeposit);
        save(MONTHLY_DEPOSIT, "ค่าประกันรายเดือน", monthlyDeposit);
        save(ELECTRIC_RATE, "หน่วยไฟ", electricRate);
        save(WATER_RATE, "หน่วยน้ำ", waterRate);
        save(FINE_AMOUNT, "ค่าปรับจ่ายบิลล่าช้า", fineAmount);
        save(FINE_INTERVAL_DAYS, "รอบวันค่าปรับ", BigDecimal.valueOf(fineIntervalDays == null || fineIntervalDays < 1 ? 1 : fineIntervalDays));
        save(CHECKOUT_OVERDUE_FINE_PER_HOUR, "ค่าปรับเช็คเอาต์เกินกำหนดต่อชั่วโมง", checkoutOverdueFinePerHour);
        saveText(DAILY_CHECKOUT_TIME, "เวลาเช็คเอาต์ห้องพักรายวัน", (dailyCheckoutTime == null ? DEFAULT_DAILY_CHECKOUT_TIME : dailyCheckoutTime).toString());
    }

    private BigDecimal value(String key) {
        return settings.findById(key)
                .map(AppSetting::getValue)
                .map(this::money)
                .orElseGet(() -> defaults().get(key));
    }

    private void save(String key, String label, BigDecimal value) {
        AppSetting setting = settings.findById(key).orElseGet(AppSetting::new);
        setting.setKey(key);
        setting.setLabel(label);
        setting.setValue(money(value));
        settings.save(setting);
    }

    private void saveText(String key, String label, String value) {
        AppSetting setting = settings.findById(key).orElseGet(AppSetting::new);
        setting.setKey(key);
        setting.setLabel(label);
        setting.setValue(BigDecimal.ZERO);
        setting.setTextValue(value);
        settings.save(setting);
    }

    private Map<String, BigDecimal> defaults() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put(DEFAULT_DEPOSIT, BigDecimal.ZERO);
        values.put(MONTHLY_DEPOSIT, BigDecimal.ZERO);
        values.put(ELECTRIC_RATE, BigDecimal.ZERO);
        values.put(WATER_RATE, BigDecimal.ZERO);
        values.put(FINE_AMOUNT, BigDecimal.ZERO);
        values.put(FINE_INTERVAL_DAYS, BigDecimal.ONE);
        values.put(CHECKOUT_OVERDUE_FINE_PER_HOUR, BigDecimal.ZERO);
        return values;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
