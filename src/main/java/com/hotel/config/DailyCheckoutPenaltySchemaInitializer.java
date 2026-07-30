package com.hotel.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DailyCheckoutPenaltySchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public DailyCheckoutPenaltySchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS t_daily_checkout_penalty_rule (
                    "ID_daily_checkout_penalty_rule" BIGSERIAL PRIMARY KEY,
                    start_day_offset INTEGER NOT NULL DEFAULT 0,
                    start_time TIME NOT NULL,
                    end_day_offset INTEGER,
                    end_time TIME,
                    id_penalty_charge_type BIGINT NOT NULL DEFAULT 2,
                    charge_value NUMERIC(12,2) NOT NULL DEFAULT 0,
                    enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    sort_order INTEGER NOT NULL DEFAULT 0
                )
                """);
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_daily_checkout_penalty_rule_sort ON t_daily_checkout_penalty_rule(sort_order)");
    }
}
