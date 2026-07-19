package com.hotel.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DepositRefundSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public DepositRefundSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS t_deposit_refund (
                    "ID_deposit_refund" BIGSERIAL PRIMARY KEY,
                    refund_no VARCHAR(30) UNIQUE,
                    refund_date DATE NOT NULL DEFAULT CURRENT_DATE,
                    "ID_guest" BIGINT NOT NULL REFERENCES t_guest("ID_guest"),
                    "ID_room" BIGINT NOT NULL REFERENCES t_room("ID_room"),
                    deposit_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                    total_deduct_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                    refund_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                    extra_charge_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                    refund_method VARCHAR(255),
                    remark VARCHAR(1000),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS t_deposit_refund_item (
                    "ID_deposit_refund_item" BIGSERIAL PRIMARY KEY,
                    "ID_deposit_refund" BIGINT NOT NULL REFERENCES t_deposit_refund("ID_deposit_refund") ON DELETE CASCADE,
                    item_name VARCHAR(255) NOT NULL,
                    item_name_en VARCHAR(255),
                    item_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                    sort_order INTEGER NOT NULL DEFAULT 0
                )
                """);
        jdbc.execute("ALTER TABLE t_deposit_refund_item ADD COLUMN IF NOT EXISTS item_name_en VARCHAR(255)");
        jdbc.update("""
                UPDATE t_deposit_refund_item
                SET item_name_en = CASE
                    WHEN item_name ILIKE '%เพิ่มค่าห้องเป็นจำนวนคืน%' OR item_name ILIKE '%คิดค่าห้องเพิ่มตามจำนวนคืน%' THEN 'Late check-out charge additional night'
                    WHEN item_name ILIKE 'ค่าปรับเช็คเอาท์%' THEN 'Late checkout penalty'
                    WHEN item_name ILIKE 'ค่าเสียหาย%' THEN 'Damage charge'
                    WHEN item_name ILIKE 'ค่าประกัน%' THEN 'Deposit charge'
                    ELSE 'Deduction item'
                END
                WHERE item_name_en IS NULL OR BTRIM(item_name_en) = ''
                """);
        jdbc.update("""
                UPDATE t_deposit_refund_item
                SET item_name_en = 'Late check-out charge additional night'
                WHERE item_name ILIKE '%เพิ่มค่าห้องเป็นจำนวนคืน%'
                   OR item_name ILIKE '%คิดค่าห้องเพิ่มตามจำนวนคืน%'
                """);
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_t_deposit_refund_no ON t_deposit_refund(refund_no)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_deposit_refund_guest ON t_deposit_refund(\"ID_guest\")");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_deposit_refund_room ON t_deposit_refund(\"ID_room\")");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_deposit_refund_item_refund ON t_deposit_refund_item(\"ID_deposit_refund\")");
        jdbc.update("""
                UPDATE t_menu
                SET m_icon = 'bi-arrow-counterclockwise'
                WHERE m_link = '/finance/deposit-refunds'
                """);
    }
}
