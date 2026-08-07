package com.hotel.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hotel.schema-init.enabled", havingValue = "true", matchIfMissing = true)
public class LookupMasterSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public LookupMasterSchemaInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_room_status (id_room_status BIGINT PRIMARY KEY, rs_code VARCHAR(40) NOT NULL UNIQUE, rs_name VARCHAR(100) NOT NULL, rs_name_en VARCHAR(100), is_active BOOLEAN NOT NULL DEFAULT TRUE)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_stay_type (id_stay_type BIGINT PRIMARY KEY, st_code VARCHAR(30) NOT NULL UNIQUE, st_name VARCHAR(100) NOT NULL, st_name_en VARCHAR(100), is_active BOOLEAN NOT NULL DEFAULT TRUE)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_advance_ledger_type (id_advance_ledger_type BIGINT PRIMARY KEY, alt_code VARCHAR(40) NOT NULL UNIQUE, alt_name VARCHAR(100) NOT NULL, alt_name_en VARCHAR(100), is_active BOOLEAN NOT NULL DEFAULT TRUE)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS t_penalty_charge_type (id_penalty_charge_type BIGINT PRIMARY KEY, pct_code VARCHAR(40) NOT NULL UNIQUE, pct_name VARCHAR(100) NOT NULL, pct_name_en VARCHAR(100), is_active BOOLEAN NOT NULL DEFAULT TRUE)");

        jdbc.update("""
                INSERT INTO t_room_status VALUES
                (1,'AVAILABLE','ว่าง','Available',true),(2,'OCCUPIED','มีผู้พัก','Occupied',true),
                (3,'DAILY_OCCUPIED','พักรายวัน','Daily occupied',true),(4,'MONTHLY_OCCUPIED','พักรายเดือน','Monthly occupied',true),
                (5,'RESERVED','จองแล้ว','Reserved',true),(6,'MAINTENANCE','ซ่อมบำรุง','Maintenance',true)
                ON CONFLICT (id_room_status) DO UPDATE SET rs_code=EXCLUDED.rs_code,rs_name=EXCLUDED.rs_name,rs_name_en=EXCLUDED.rs_name_en
                """);
        jdbc.update("""
                INSERT INTO t_stay_type VALUES
                (1,'DAILY','รายวัน','Daily',true),(2,'MONTHLY','รายเดือน','Monthly',true)
                ON CONFLICT (id_stay_type) DO UPDATE SET st_code=EXCLUDED.st_code,st_name=EXCLUDED.st_name,st_name_en=EXCLUDED.st_name_en
                """);
        jdbc.update("""
                INSERT INTO t_advance_ledger_type VALUES
                (1,'ADD_ADVANCE','เพิ่มเงินล่วงหน้า','Add advance',true),(2,'APPLY_ADVANCE','นำเงินล่วงหน้าไปหักบิล','Apply advance',true),
                (3,'REFUND_ADVANCE','คืนเงินล่วงหน้า','Refund advance',true),(4,'ADJUST_ADVANCE','ปรับยอดเงินล่วงหน้า','Adjust advance',true)
                ON CONFLICT (id_advance_ledger_type) DO UPDATE SET alt_code=EXCLUDED.alt_code,alt_name=EXCLUDED.alt_name,alt_name_en=EXCLUDED.alt_name_en
                """);
        jdbc.update("""
                INSERT INTO t_penalty_charge_type VALUES
                (1,'FREE','ไม่คิดค่าบริการ','Free',true),(2,'FIXED','จำนวนเงินคงที่','Fixed amount',true),
                (3,'PER_HOUR','คิดต่อชั่วโมง','Per hour',true),(4,'PERCENT_DAILY_RATE','เปอร์เซ็นต์ของค่าห้องรายวัน','Percent of daily rate',true),
                (5,'ADD_NIGHTS','เพิ่มค่าห้องเป็นจำนวนคืน','Additional nights',true)
                ON CONFLICT (id_penalty_charge_type) DO UPDATE SET pct_code=EXCLUDED.pct_code,pct_name=EXCLUDED.pct_name,pct_name_en=EXCLUDED.pct_name_en
                """);

        jdbc.execute("ALTER TABLE t_room ADD COLUMN IF NOT EXISTS id_room_status BIGINT DEFAULT 1");
        jdbc.execute("ALTER TABLE t_booking ADD COLUMN IF NOT EXISTS id_stay_type BIGINT DEFAULT 1");
        jdbc.execute("ALTER TABLE t_guest ADD COLUMN IF NOT EXISTS id_stay_type BIGINT DEFAULT 1");
        jdbc.execute("ALTER TABLE t_room_transfer ADD COLUMN IF NOT EXISTS id_stay_type BIGINT DEFAULT 1");
        jdbc.execute("ALTER TABLE t_advance_ledger ADD COLUMN IF NOT EXISTS id_advance_ledger_type BIGINT DEFAULT 1");
        jdbc.execute("ALTER TABLE t_daily_checkout_penalty_rule ADD COLUMN IF NOT EXISTS id_penalty_charge_type BIGINT DEFAULT 2");

        addFk("t_room", "fk_t_room_status", "id_room_status", "t_room_status", "id_room_status");
        addFk("t_booking", "fk_t_booking_stay_type", "id_stay_type", "t_stay_type", "id_stay_type");
        addFk("t_guest", "fk_t_guest_stay_type", "id_stay_type", "t_stay_type", "id_stay_type");
        addFk("t_room_transfer", "fk_t_room_transfer_stay_type", "id_stay_type", "t_stay_type", "id_stay_type");
        addFk("t_advance_ledger", "fk_t_advance_ledger_type", "id_advance_ledger_type", "t_advance_ledger_type", "id_advance_ledger_type");
        addFk("t_daily_checkout_penalty_rule", "fk_t_penalty_charge_type", "id_penalty_charge_type", "t_penalty_charge_type", "id_penalty_charge_type");
    }

    private void addFk(String table, String constraint, String column, String refTable, String refColumn) {
        jdbc.execute("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='" + constraint + "') THEN ALTER TABLE " + table + " ADD CONSTRAINT " + constraint + " FOREIGN KEY (" + column + ") REFERENCES " + refTable + "(" + refColumn + "); END IF; END $$");
    }
}
