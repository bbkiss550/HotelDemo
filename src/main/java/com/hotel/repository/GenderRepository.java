package com.hotel.repository;

import com.hotel.model.Gender;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenderRepository extends JpaRepository<Gender, Long> {
    Optional<Gender> findByName(String name);
}
