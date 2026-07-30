package com.hotel.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookingStatusSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public BookingStatusSchemaInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS t_booking_status (
                    id_booking_status BIGINT PRIMARY KEY,
                    bs_code VARCHAR(30) NOT NULL UNIQUE,
                    bs_name VARCHAR(100) NOT NULL,
                    bs_name_en VARCHAR(100),
                    is_active BOOLEAN NOT NULL DEFAULT TRUE
                )
                """);
        jdbc.update("""
                INSERT INTO t_booking_status (id_booking_status, bs_code, bs_name, bs_name_en)
                VALUES (1, 'PENDING', 'รอเข้าพัก', 'Waiting for check-in'),
                       (3, 'CHECKED_IN', 'เข้าพักแล้ว', 'Checked in'),
                       (4, 'CANCELLED', 'ยกเลิก', 'Cancelled')
                ON CONFLICT (id_booking_status) DO UPDATE SET
                    bs_code = EXCLUDED.bs_code, bs_name = EXCLUDED.bs_name, bs_name_en = EXCLUDED.bs_name_en
                """);
        jdbc.execute("ALTER TABLE t_booking ADD COLUMN IF NOT EXISTS id_booking_status BIGINT");
        jdbc.update("UPDATE t_booking SET id_booking_status = 1 WHERE id_booking_status = 2 OR id_booking_status IS NULL");
        jdbc.update("DELETE FROM t_booking_status WHERE id_booking_status = 2");
        jdbc.execute("""
                DO $$ BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_t_booking_status') THEN
                        ALTER TABLE t_booking ADD CONSTRAINT fk_t_booking_status
                        FOREIGN KEY (id_booking_status) REFERENCES t_booking_status(id_booking_status);
                    END IF;
                END $$
                """);
        jdbc.execute("DROP TRIGGER IF EXISTS trg_sync_booking_status_master ON t_booking");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_booking_status ON t_booking(id_booking_status)");
    }
}
