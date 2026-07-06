package com.hotel.controller;

import com.hotel.model.Menu;
import com.hotel.repository.MenuRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/menus")
public class MenuController {
    private final MenuRepository menus;

    public MenuController(MenuRepository menus) {
        this.menus = menus;
    }

    @GetMapping("/order")
    String order(Model model) {
        List<Menu> allMenus = menus.findAllByOrderByParentIdAscSortOrderAscIdAsc();
        Map<Long, Menu> byId = new LinkedHashMap<>();
        allMenus.forEach(menu -> {
            menu.setChildren(new ArrayList<>());
            byId.put(menu.getId(), menu);
        });

        List<Menu> roots = new ArrayList<>();
        allMenus.forEach(menu -> {
            if (menu.getParentId() == null || !byId.containsKey(menu.getParentId())) {
                roots.add(menu);
            } else {
                byId.get(menu.getParentId()).getChildren().add(menu);
            }
        });

        model.addAttribute("menus", roots);
        return "menus/order";
    }

    @PostMapping("/order/reorder")
    @ResponseBody
    ResponseEntity<String> reorder(@RequestParam(required = false) Long parentId, @RequestParam List<Long> menuIds) {
        if (parentId != null && menus.findById(parentId).isEmpty()) {
            return ResponseEntity.badRequest().body("parent not found");
        }
        for (int i = 0; i < menuIds.size(); i++) {
            Menu menu = menus.findById(menuIds.get(i)).orElseThrow();
            if (isInvalidParent(menu.getId(), parentId)) {
                return ResponseEntity.badRequest().body("invalid parent");
            }
            menu.setParentId(parentId);
            menu.setSortOrder((i + 1) * 10);
            menus.save(menu);
        }
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/order/link")
    @ResponseBody
    ResponseEntity<String> updateLink(@RequestParam Long menuId, @RequestParam(defaultValue = "") String link) {
        Menu menu = menus.findById(menuId).orElseThrow();
        String normalizedLink = link == null ? "" : link.trim();
        if (!normalizedLink.isBlank()
                && !normalizedLink.startsWith("/")
                && !normalizedLink.startsWith("http://")
                && !normalizedLink.startsWith("https://")
                && !normalizedLink.equals("#")) {
            return ResponseEntity.badRequest().body("invalid link");
        }
        menu.setLink(normalizedLink.isBlank() || normalizedLink.equals("#") ? null : normalizedLink);
        menus.save(menu);
        return ResponseEntity.ok("ok");
    }

    private boolean isInvalidParent(Long menuId, Long parentId) {
        Long currentParentId = parentId;
        while (currentParentId != null) {
            if (currentParentId.equals(menuId)) {
                return true;
            }
            currentParentId = menus.findById(currentParentId)
                    .map(Menu::getParentId)
                    .orElse(null);
        }
        return false;
    }
}
