package com.hotel.repository;

import com.hotel.model.DailyCheckoutPenaltyRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCheckoutPenaltyRuleRepository extends JpaRepository<DailyCheckoutPenaltyRule, Long> {
    List<DailyCheckoutPenaltyRule> findAllByOrderBySortOrderAscIdAsc();
}
