package com.hotel.service;

import com.hotel.model.AppSetting;
import com.hotel.repository.AppSettingRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppSettingService {
    public static final String DEFAULT_DEPOSIT = "default_deposit";
    public static final String ELECTRIC_RATE = "electric_rate";
    public static final String WATER_RATE = "water_rate";
    public static final String FINE_AMOUNT = "fine_amount";
    public static final String FINE_INTERVAL_DAYS = "fine_interval_days";
    public static final String SYSTEM_NAME = "system_name";
    private static final String DEFAULT_SYSTEM_NAME = "BlueCatHotelDemo";

    private final AppSettingRepository settings;

    public AppSettingService(AppSettingRepository settings) {
        this.settings = settings;
    }

    public Map<String, BigDecimal> values() {
        Map<String, BigDecimal> values = defaults();
        settings.findAll().forEach(setting -> values.put(setting.getKey(), money(setting.getValue())));
        return values;
    }

    public BigDecimal defaultDeposit() {
        return value(DEFAULT_DEPOSIT);
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

    public String systemName() {
        return settings.findById(SYSTEM_NAME)
                .map(AppSetting::getTextValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(DEFAULT_SYSTEM_NAME);
    }

    @Transactional
    public void saveDefaults(String systemName, BigDecimal defaultDeposit, BigDecimal electricRate, BigDecimal waterRate, BigDecimal fineAmount, Integer fineIntervalDays) {
        saveText(SYSTEM_NAME, "ชื่อระบบ", systemName == null || systemName.isBlank() ? DEFAULT_SYSTEM_NAME : systemName.trim());
        save(DEFAULT_DEPOSIT, "ค่าประกัน", defaultDeposit);
        save(ELECTRIC_RATE, "หน่วยไฟ", electricRate);
        save(WATER_RATE, "หน่วยน้ำ", waterRate);
        save(FINE_AMOUNT, "ค่าปรับ", fineAmount);
        save(FINE_INTERVAL_DAYS, "รอบวันค่าปรับ", BigDecimal.valueOf(fineIntervalDays == null || fineIntervalDays < 1 ? 1 : fineIntervalDays));
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
        values.put(ELECTRIC_RATE, BigDecimal.ZERO);
        values.put(WATER_RATE, BigDecimal.ZERO);
        values.put(FINE_AMOUNT, BigDecimal.ZERO);
        values.put(FINE_INTERVAL_DAYS, BigDecimal.ONE);
        return values;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
