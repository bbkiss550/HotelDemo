package com.hotel.controller;

import com.hotel.model.AppUser;
import com.hotel.repository.AppUserRepository;
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
    private final PasswordEncoder encoder;

    public UserController(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @GetMapping
    String index(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("users", q.isBlank() ? users.findAll() : users.search(q));
        model.addAttribute("roles", java.util.List.of("ADMIN", "STAFF", "REPORT"));
        return "users/index";
    }

    @PostMapping
    String save(@ModelAttribute AppUser formUser, @RequestParam(defaultValue = "false") boolean enabled, RedirectAttributes redirect, Model model) {
        boolean creating = formUser.getId() == null;
        AppUser user = creating ? new AppUser() : users.findById(formUser.getId()).orElseThrow();

        if (creating && users.existsByUsername(formUser.getUsername())) {
            redirect.addFlashAttribute("error", "username นี้ถูกใช้งานแล้ว");
            redirect.addFlashAttribute("flashType", "warning");
            return "redirect:/users";
        }
        if (!creating && !user.getUsername().equals(formUser.getUsername()) && users.existsByUsername(formUser.getUsername())) {
            redirect.addFlashAttribute("error", "username นี้ถูกใช้งานแล้ว");
            redirect.addFlashAttribute("flashType", "warning");
            return "redirect:/users";
        }
        if (creating && (formUser.getPassword() == null || formUser.getPassword().isBlank())) {
            redirect.addFlashAttribute("error", "กรุณากรอกรหัสผ่าน");
            redirect.addFlashAttribute("flashType", "warning");
            return "redirect:/users";
        }

        user.setUsername(formUser.getUsername());
        user.setFullName(formUser.getFullName());
        user.setRole(formUser.getRole() == null || formUser.getRole().isBlank() ? "STAFF" : formUser.getRole());
        user.setEnabled(enabled);
        if (formUser.getPassword() != null && !formUser.getPassword().isBlank()) {
            user.setPassword(encoder.encode(formUser.getPassword()));
        }
        users.save(user);
        redirect.addFlashAttribute("message", "บันทึกข้อมูลผู้ใช้แล้ว");
        redirect.addFlashAttribute("flashType", creating ? "success" : "edit");
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        AppUser user = users.findById(id).orElseThrow();
        if (users.count() <= 1) {
            redirect.addFlashAttribute("error", "ไม่สามารถลบผู้ใช้คนสุดท้ายของระบบได้");
            redirect.addFlashAttribute("flashType", "warning");
            return "redirect:/users";
        }
        if (principal != null && user.getUsername().equals(principal.getName())) {
            redirect.addFlashAttribute("error", "ไม่สามารถลบบัญชีที่กำลังใช้งานอยู่ได้");
            redirect.addFlashAttribute("flashType", "warning");
            return "redirect:/users";
        }
        users.delete(user);
        redirect.addFlashAttribute("message", "ลบผู้ใช้แล้ว");
        redirect.addFlashAttribute("flashType", "delete");
        return "redirect:/users";
    }

}
