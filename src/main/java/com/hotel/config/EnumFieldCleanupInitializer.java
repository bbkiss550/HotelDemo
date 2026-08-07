package com.hotel.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Migrates old enum-like text columns once, then removes them permanently. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "hotel.schema-init.enabled", havingValue = "true", matchIfMissing = true)
public class EnumFieldCleanupInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public EnumFieldCleanupInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("ALTER TABLE t_room ADD COLUMN IF NOT EXISTS id_room_status BIGINT DEFAULT 1");
        jdbc.execute("ALTER TABLE t_booking ADD COLUMN IF NOT EXISTS id_stay_type BIGINT DEFAULT 1");
        jdbc.execute("ALTER TABLE t_guest ADD COLUMN IF NOT EXISTS id_stay_type BIGINT DEFAULT 1");
        jdbc.execute("ALTER TABLE t_room_transfer ADD COLUMN IF NOT EXISTS id_stay_type BIGINT DEFAULT 1");
        jdbc.execute("ALTER TABLE t_advance_ledger ADD COLUMN IF NOT EXISTS id_advance_ledger_type BIGINT DEFAULT 1");
        jdbc.execute("ALTER TABLE t_daily_checkout_penalty_rule ADD COLUMN IF NOT EXISTS id_penalty_charge_type BIGINT DEFAULT 2");
        jdbc.execute("ALTER TABLE t_payment ADD COLUMN IF NOT EXISTS id_payment_status BIGINT DEFAULT 2");

        jdbc.execute("""
                DO $$ BEGIN
                    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='t_room' AND column_name='r_status') THEN
                        UPDATE t_room r SET id_room_status=s.id_room_status FROM t_room_status s WHERE s.rs_code=upper(coalesce(r.r_status,'AVAILABLE'));
                    END IF;
                    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='t_booking' AND column_name='b_stay_type') THEN
                        UPDATE t_booking b SET id_stay_type=s.id_stay_type FROM t_stay_type s WHERE s.st_code=upper(coalesce(b.b_stay_type,'DAILY'));
                    END IF;
                    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='t_guest' AND column_name='g_stay_type') THEN
                        UPDATE t_guest g SET id_stay_type=s.id_stay_type FROM t_stay_type s WHERE s.st_code=upper(coalesce(g.g_stay_type,'DAILY'));
                    END IF;
                    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='t_room_transfer' AND column_name='stay_type') THEN
                        UPDATE t_room_transfer r SET id_stay_type=s.id_stay_type FROM t_stay_type s WHERE s.st_code=upper(coalesce(r.stay_type,'DAILY'));
                    END IF;
                    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='t_advance_ledger' AND column_name='ledger_type') THEN
                        UPDATE t_advance_ledger a SET id_advance_ledger_type=s.id_advance_ledger_type FROM t_advance_ledger_type s WHERE s.alt_code=upper(coalesce(a.ledger_type,'ADD_ADVANCE'));
                    END IF;
                    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='t_daily_checkout_penalty_rule' AND column_name='charge_type') THEN
                        UPDATE t_daily_checkout_penalty_rule p SET id_penalty_charge_type=s.id_penalty_charge_type FROM t_penalty_charge_type s WHERE s.pct_code=upper(coalesce(p.charge_type,'FIXED'));
                    END IF;
                    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='t_booking' AND column_name='b_status') THEN
                        UPDATE t_booking SET b_status='PENDING' WHERE b_status='CONFIRMED';
                        UPDATE t_booking SET id_booking_status=case upper(coalesce(b_status,'PENDING')) when 'CHECKED_IN' then 3 when 'CANCELLED' then 4 else 1 end;
                    END IF;
                    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='t_payment' AND column_name='p_status') THEN
                        UPDATE t_payment SET id_payment_status=case upper(coalesce(p_status,'UNPAID')) when 'PAID' then 1 when 'PARTIAL' then 3 when 'VOID' then 4 else 2 end;
                    END IF;
                END $$
                """);

        jdbc.execute("DROP TRIGGER IF EXISTS trg_sync_booking_status_master ON t_booking");
        jdbc.execute("ALTER TABLE t_room DROP COLUMN IF EXISTS r_status");
        jdbc.execute("ALTER TABLE t_booking DROP COLUMN IF EXISTS b_status");
        jdbc.execute("ALTER TABLE t_booking DROP COLUMN IF EXISTS b_stay_type");
        jdbc.execute("ALTER TABLE t_guest DROP COLUMN IF EXISTS g_stay_type");
        jdbc.execute("ALTER TABLE t_room_transfer DROP COLUMN IF EXISTS stay_type");
        jdbc.execute("ALTER TABLE t_advance_ledger DROP COLUMN IF EXISTS ledger_type");
        jdbc.execute("ALTER TABLE t_daily_checkout_penalty_rule DROP COLUMN IF EXISTS charge_type");
        jdbc.execute("ALTER TABLE t_payment DROP COLUMN IF EXISTS p_status");
    }
}
