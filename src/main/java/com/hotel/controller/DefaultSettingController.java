package com.hotel.controller;

import com.hotel.service.AppSettingService;
import com.hotel.service.DailyCheckoutPenaltyService;
import com.hotel.model.DailyCheckoutPenaltyChargeType;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings/defaults")
public class DefaultSettingController {
    private final AppSettingService settings;
    private final DailyCheckoutPenaltyService checkoutPenalties;

    public DefaultSettingController(AppSettingService settings, DailyCheckoutPenaltyService checkoutPenalties) {
        this.settings = settings;
        this.checkoutPenalties = checkoutPenalties;
    }

    @GetMapping
    String index(Model model) {
        model.addAttribute("settings", settings.values());
        model.addAttribute("systemNameValue", settings.systemName());
        model.addAttribute("fineIntervalDaysValue", settings.fineIntervalDays());
        model.addAttribute("dailyCheckoutTime", settings.dailyCheckoutTime());
        model.addAttribute("dailyCheckoutPenaltyRules", checkoutPenalties.rules());
        model.addAttribute("dailyCheckoutPenaltyChargeTypes", DailyCheckoutPenaltyChargeType.values());
        return "settings/defaults";
    }

    @PostMapping
    String save(@RequestParam(defaultValue = "BlueCatHotelDemo") String systemName,
                @RequestParam(defaultValue = "0") BigDecimal defaultDeposit,
                @RequestParam(defaultValue = "0") BigDecimal monthlyDeposit,
                @RequestParam(defaultValue = "0") BigDecimal electricRate,
                @RequestParam(defaultValue = "0") BigDecimal waterRate,
                @RequestParam(defaultValue = "0") BigDecimal fineAmount,
                @RequestParam(defaultValue = "1") Integer fineIntervalDays,
                @RequestParam(defaultValue = "0") BigDecimal checkoutOverdueFinePerHour,
                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime dailyCheckoutTime,
                @RequestParam(required = false) List<Long> ruleId,
                @RequestParam(required = false) List<String> ruleStartTime,
                @RequestParam(required = false) List<String> ruleEndTime,
                @RequestParam(required = false) List<String> ruleChargeType,
                @RequestParam(required = false) List<BigDecimal> ruleChargeValue,
                @RequestParam(required = false) List<String> ruleEnabled,
                RedirectAttributes redirect) {
        try {
            checkoutPenalties.saveRules(ruleId, ruleStartTime, ruleEndTime, ruleChargeType, ruleChargeValue, ruleEnabled);
            settings.saveDefaults(systemName, defaultDeposit, monthlyDeposit, electricRate, waterRate, fineAmount, fineIntervalDays, checkoutOverdueFinePerHour, dailyCheckoutTime);
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            redirect.addFlashAttribute("flashType", "warning");
            return "redirect:/settings/defaults";
        }
        redirect.addFlashAttribute("message", "บันทึกตั้งค่าเริ่มต้นเรียบร้อย");
        redirect.addFlashAttribute("flashType", "success");
        return "redirect:/settings/defaults";
    }
}
