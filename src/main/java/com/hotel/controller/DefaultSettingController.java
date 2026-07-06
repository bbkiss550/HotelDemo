package com.hotel.controller;

import com.hotel.service.AppSettingService;
import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
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

    public DefaultSettingController(AppSettingService settings) {
        this.settings = settings;
    }

    @GetMapping
    String index(Model model) {
        model.addAttribute("settings", settings.values());
        model.addAttribute("systemNameValue", settings.systemName());
        model.addAttribute("fineIntervalDaysValue", settings.fineIntervalDays());
        return "settings/defaults";
    }

    @PostMapping
    String save(@RequestParam(defaultValue = "BlueCatHotelDemo") String systemName,
                @RequestParam(defaultValue = "0") BigDecimal defaultDeposit,
                @RequestParam(defaultValue = "0") BigDecimal electricRate,
                @RequestParam(defaultValue = "0") BigDecimal waterRate,
                @RequestParam(defaultValue = "0") BigDecimal fineAmount,
                @RequestParam(defaultValue = "1") Integer fineIntervalDays,
                RedirectAttributes redirect) {
        settings.saveDefaults(systemName, defaultDeposit, electricRate, waterRate, fineAmount, fineIntervalDays);
        redirect.addFlashAttribute("message", "บันทึกตั้งค่าเริ่มต้นเรียบร้อย");
        redirect.addFlashAttribute("flashType", "success");
        return "redirect:/settings/defaults";
    }
}
