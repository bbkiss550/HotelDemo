package com.hotel.repository;

import com.hotel.model.Menu;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findAllByOrderBySortOrderAscIdAsc();
    List<Menu> findAllByOrderByParentIdAscSortOrderAscIdAsc();
    List<Menu> findByParentIdOrderBySortOrderAscIdAsc(Long parentId);
    Optional<Menu> findByLink(String link);
}
