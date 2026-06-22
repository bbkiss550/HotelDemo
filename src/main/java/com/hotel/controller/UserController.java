package com.hotel.controller;

import com.hotel.model.AppUser;
import com.hotel.repository.AppUserRepository;
import com.hotel.repository.GenderRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;

@Controller
@RequestMapping("/users")
public class UserController {
    private final AppUserRepository users;
    private final GenderRepository genders;
    private final PasswordEncoder encoder;

    public UserController(AppUserRepository users, GenderRepository genders, PasswordEncoder encoder) {
        this.users = users;
        this.genders = genders;
        this.encoder = encoder;
    }

    @GetMapping
    String index(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("users", q.isBlank() ? users.findAll() : users.search(q));
        return "users/index";
    }

    @GetMapping("/new")
    String create(Model model) {
        formData(model, new AppUser());
        return "users/form";
    }

    @GetMapping("/{id}/edit")
    String edit(@PathVariable Long id, Model model) {
        formData(model, users.findById(id).orElseThrow());
        return "users/form";
    }

    @PostMapping
    String save(@ModelAttribute AppUser formUser, @RequestParam(required = false) Long genderId, RedirectAttributes redirect, Model model) {
        boolean creating = formUser.getId() == null;
        AppUser user = creating ? new AppUser() : users.findById(formUser.getId()).orElseThrow();

        if (creating && users.existsByUsername(formUser.getUsername())) {
            model.addAttribute("error", "username นี้ถูกใช้งานแล้ว");
            formData(model, formUser);
            return "users/form";
        }
        if (!creating && !user.getUsername().equals(formUser.getUsername()) && users.existsByUsername(formUser.getUsername())) {
            model.addAttribute("error", "username นี้ถูกใช้งานแล้ว");
            formData(model, formUser);
            return "users/form";
        }
        if (creating && (formUser.getPassword() == null || formUser.getPassword().isBlank())) {
            model.addAttribute("error", "กรุณากรอกรหัสผ่าน");
            formData(model, formUser);
            return "users/form";
        }

        user.setUsername(formUser.getUsername());
        user.setFullName(formUser.getFullName());
        user.setGender(genderId == null ? null : genders.findById(genderId).orElse(null));
        user.setBirthDate(formUser.getBirthDate());
        user.setRole(formUser.getRole() == null || formUser.getRole().isBlank() ? "STAFF" : formUser.getRole());
        user.setEnabled(formUser.getEnabled() == null || formUser.getEnabled());
        if (formUser.getPassword() != null && !formUser.getPassword().isBlank()) {
            user.setPassword(encoder.encode(formUser.getPassword()));
        }
        users.save(user);
        redirect.addFlashAttribute("message", "บันทึกข้อมูลผู้ใช้แล้ว");
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        AppUser user = users.findById(id).orElseThrow();
        if (users.count() <= 1) {
            redirect.addFlashAttribute("error", "ไม่สามารถลบผู้ใช้คนสุดท้ายของระบบได้");
            return "redirect:/users";
        }
        if (principal != null && user.getUsername().equals(principal.getName())) {
            redirect.addFlashAttribute("error", "ไม่สามารถลบบัญชีที่กำลังใช้งานอยู่ได้");
            return "redirect:/users";
        }
        users.delete(user);
        redirect.addFlashAttribute("message", "ลบผู้ใช้แล้ว");
        return "redirect:/users";
    }

    private void formData(Model model, AppUser user) {
        user.setPassword("");
        model.addAttribute("user", user);
        model.addAttribute("genders", genders.findAll());
        model.addAttribute("roles", java.util.List.of("ADMIN", "STAFF", "REPORT"));
    }
}
