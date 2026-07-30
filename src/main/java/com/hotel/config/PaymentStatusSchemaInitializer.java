package com.hotel.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentStatusSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public PaymentStatusSchemaInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS t_payment_status (
                    id_payment_status BIGINT PRIMARY KEY,
                    ps_code VARCHAR(30) NOT NULL UNIQUE,
                    ps_name VARCHAR(100) NOT NULL,
                    ps_name_en VARCHAR(100),
                    is_active BOOLEAN NOT NULL DEFAULT TRUE
                )
                """);
        jdbc.update("""
                INSERT INTO t_payment_status (id_payment_status, ps_code, ps_name, ps_name_en)
                VALUES (1, 'PAID', 'à¸Šà¸³à¸£à¸°à¹à¸¥à¹‰à¸§', 'Paid'),
                       (2, 'UNPAID', 'à¸„à¹‰à¸²à¸‡à¸Šà¸³à¸£à¸°', 'Unpaid'),
                       (3, 'PARTIAL', 'à¸Šà¸³à¸£à¸°à¸šà¸²à¸‡à¸ªà¹ˆà¸§à¸™', 'Partially paid'),
                       (4, 'VOID', 'à¸¢à¸à¹€à¸¥à¸´à¸', 'Voided')
                ON CONFLICT (id_payment_status) DO UPDATE SET
                    ps_code = EXCLUDED.ps_code, ps_name = EXCLUDED.ps_name, ps_name_en = EXCLUDED.ps_name_en
                """);
        jdbc.execute("""
                UPDATE t_payment_status SET ps_name = convert_from(decode('E0B88AE0B8B3E0B8A3E0B8B0E0B981E0B8A5E0B989E0B8A7', 'hex'), 'UTF8') WHERE id_payment_status = 1;
                UPDATE t_payment_status SET ps_name = convert_from(decode('E0B884E0B989E0B8B2E0B887E0B88AE0B8B3E0B8A3E0B8B0', 'hex'), 'UTF8') WHERE id_payment_status = 2;
                UPDATE t_payment_status SET ps_name = convert_from(decode('E0B88AE0B8B3E0B8A3E0B8B0E0B89AE0B8B2E0B887E0B8AAE0B988E0B8A7E0B899', 'hex'), 'UTF8') WHERE id_payment_status = 3;
                UPDATE t_payment_status SET ps_name = convert_from(decode('E0B8A2E0B881E0B980E0B8A5E0B8B4E0B881', 'hex'), 'UTF8') WHERE id_payment_status = 4;
                """);
        jdbc.execute("ALTER TABLE t_payment ADD COLUMN IF NOT EXISTS id_payment_status BIGINT DEFAULT 2");
        jdbc.execute("""
                DO $$ BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_t_payment_status') THEN
                        ALTER TABLE t_payment ADD CONSTRAINT fk_t_payment_status
                        FOREIGN KEY (id_payment_status) REFERENCES t_payment_status(id_payment_status);
                    END IF;
                END $$
                """);
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_payment_status ON t_payment(id_payment_status)");
    }
}
