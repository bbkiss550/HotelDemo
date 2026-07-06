package com.hotel.repository;

import com.hotel.model.Reciept;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecieptRepository extends JpaRepository<Reciept, Long> {
    Optional<Reciept> findTopByRecieptNoStartingWithOrderByRecieptNoDesc(String prefix);
}
