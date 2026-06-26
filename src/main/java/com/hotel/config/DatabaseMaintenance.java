package com.hotel.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseMaintenance {
    @Bean
    CommandLineRunner relaxBookingRoomColumn(JdbcTemplate jdbc) {
        return args -> {
            try {
                jdbc.execute("ALTER TABLE t_booking ALTER COLUMN \"ID_room\" DROP NOT NULL");
            } catch (Exception ignored) {
                // The column may already be nullable.
            }
            try {
                jdbc.update("""
                        UPDATE t_booking b
                        SET "ID_room_type" = (
                            SELECT r."ID_room_type"
                            FROM t_room r
                            WHERE r."ID_room" = b."ID_room"
                        )
                        WHERE b."ID_room_type" IS NULL
                          AND b."ID_room" IS NOT NULL
                        """);
            } catch (Exception ignored) {
                // Existing databases may already have the new booking room type values.
            }
        };
    }
}
