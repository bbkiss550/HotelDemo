package com.hotel.config;

import com.hotel.model.Menu;
import com.hotel.repository.MenuRepository;
import com.hotel.service.AppSettingService;
import com.hotel.service.HotelNotificationService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class MenuModelAdvice {
    private final MenuRepository menus;
    private final AppSettingService settings;
    private final HotelNotificationService notifications;

    public MenuModelAdvice(MenuRepository menus, AppSettingService settings, HotelNotificationService notifications) {
        this.menus = menus;
        this.settings = settings;
        this.notifications = notifications;
    }

    @ModelAttribute("sidebarMenus")
    List<Menu> sidebarMenus() {
        List<Menu> allMenus = menus.findAllByOrderByParentIdAscSortOrderAscIdAsc().stream()
                .filter(menu -> "A".equalsIgnoreCase(menu.getStatus()))
                .toList();
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
        return roots;
    }

    @ModelAttribute("systemName")
    String systemName() {
        return settings.systemName();
    }

    @ModelAttribute("notificationSummary")
    HotelNotificationService.NotificationSummary notificationSummary() {
        return notifications.summary();
    }

    @ModelAttribute("currentMenuName")
    String currentMenuName(HttpServletRequest request) {
        String path = normalizePath(request.getRequestURI());
        return menus.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(menu -> menu.getLink() != null && !menu.getLink().isBlank())
                .filter(menu -> path.equals(normalizePath(menu.getLink())) || path.startsWith(normalizePath(menu.getLink()) + "/"))
                .sorted((left, right) -> Integer.compare(normalizePath(right.getLink()).length(), normalizePath(left.getLink()).length()))
                .map(Menu::getName)
                .findFirst()
                .orElse("");
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalized = path.trim();
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isBlank() ? "/" : normalized;
    }
}
