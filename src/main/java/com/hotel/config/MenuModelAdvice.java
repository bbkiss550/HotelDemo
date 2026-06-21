package com.hotel.config;

import com.hotel.model.Menu;
import com.hotel.repository.MenuRepository;
import java.util.List;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class MenuModelAdvice {
    private final MenuRepository menus;

    public MenuModelAdvice(MenuRepository menus) {
        this.menus = menus;
    }

    @ModelAttribute("sidebarMenus")
    List<Menu> sidebarMenus() {
        return menus.findAllByOrderBySortOrderAscIdAsc();
    }
}
