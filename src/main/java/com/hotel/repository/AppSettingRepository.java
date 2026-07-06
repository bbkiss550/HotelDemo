package com.hotel.repository;

import com.hotel.model.AppSetting;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
    List<AppSetting> findAllByOrderByKeyAsc();
}
