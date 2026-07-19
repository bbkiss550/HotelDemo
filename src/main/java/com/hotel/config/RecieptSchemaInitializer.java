package com.hotel.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RecieptSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public RecieptSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS t_reciept_type (
                    "ID_rec_type" BIGINT PRIMARY KEY,
                    rec_type_name VARCHAR(255) NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS t_reciept (
                    "ID_reciept" BIGSERIAL PRIMARY KEY,
                    rec_date DATE NOT NULL DEFAULT CURRENT_DATE,
                    rec_no VARCHAR(30) UNIQUE,
                    "ID_payment" BIGINT,
                    "ID_rec_type" BIGINT NOT NULL REFERENCES t_reciept_type("ID_rec_type"),
                    rec_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbc.execute("ALTER TABLE t_reciept ADD COLUMN IF NOT EXISTS rec_no VARCHAR(30)");
        jdbc.execute("ALTER TABLE t_reciept ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        jdbc.execute("ALTER TABLE t_reciept ADD COLUMN IF NOT EXISTS \"ID_payment\" BIGINT");
        jdbc.execute("ALTER TABLE t_payment ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        jdbc.execute("ALTER TABLE t_payment ALTER COLUMN \"ID_room\" DROP NOT NULL");
        jdbc.execute("ALTER TABLE t_payment ALTER COLUMN \"ID_guest\" DROP NOT NULL");
        jdbc.execute("ALTER TABLE t_payment ADD COLUMN IF NOT EXISTS \"ID_reciept\" BIGINT");
        jdbc.execute("ALTER TABLE t_payment ADD COLUMN IF NOT EXISTS ID_monthly_rent_bill BIGINT");
        jdbc.execute("ALTER TABLE t_payment ADD COLUMN IF NOT EXISTS \"ID_booking\" BIGINT");
        jdbc.execute("ALTER TABLE t_payment ADD COLUMN IF NOT EXISTS \"ID_deposit_refund\" BIGINT");
        jdbc.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_t_payment_booking') THEN
                        ALTER TABLE t_payment ADD CONSTRAINT fk_t_payment_booking
                        FOREIGN KEY ("ID_booking") REFERENCES t_booking("ID_booking");
                    END IF;
                    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_t_payment_deposit_refund') THEN
                        ALTER TABLE t_payment ADD CONSTRAINT fk_t_payment_deposit_refund
                        FOREIGN KEY ("ID_deposit_refund") REFERENCES t_deposit_refund("ID_deposit_refund");
                    END IF;
                END $$
                """);
        jdbc.execute("""
                UPDATE t_payment p
                SET "ID_booking" = b."ID_booking"
                FROM t_booking b
                WHERE p."ID_booking" IS NULL
                  AND p.p_remark LIKE '%' || b.b_booking_number || '%'
                """);
        jdbc.execute("""
                UPDATE t_payment p
                SET "ID_deposit_refund" = r."ID_deposit_refund"
                FROM t_deposit_refund r
                WHERE p."ID_deposit_refund" IS NULL
                  AND p.p_remark LIKE '%' || r.refund_no || '%'
                """);
        jdbc.execute("""
                DO $$
                BEGIN
                    IF to_regclass('t_payemnt_item') IS NOT NULL
                       AND to_regclass('t_payment_item') IS NULL THEN
                        ALTER TABLE t_payemnt_item RENAME TO t_payment_item;
                        ALTER TABLE t_payment_item RENAME COLUMN "ID_payemnt_item" TO "ID_payment_item";
                        ALTER TABLE t_payment_detail RENAME COLUMN "ID_payemnt_item" TO "ID_payment_item";
                    END IF;
                END $$
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS t_payment_item (
                    "ID_payment_item" BIGSERIAL PRIMARY KEY,
                    item_name VARCHAR(255) NOT NULL,
                    item_name_en VARCHAR(255),
                    is_active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS t_payment_detail (
                    "ID_payment_detail" BIGSERIAL PRIMARY KEY,
                    "ID_payment" BIGINT NOT NULL REFERENCES t_payment("ID_payment"),
                    "ID_payment_item" BIGINT REFERENCES t_payment_item("ID_payment_item"),
                    detail_quantity NUMERIC(12,2) NOT NULL DEFAULT 1,
                    detail_unit_price NUMERIC(12,2) NOT NULL DEFAULT 0,
                    detail_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
                    sort_order INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbc.execute("ALTER TABLE t_payment_detail ADD COLUMN IF NOT EXISTS \"ID_payment_item\" BIGINT");
        jdbc.execute("ALTER TABLE t_payment_detail DROP COLUMN IF EXISTS detail_remark");
        jdbc.execute("ALTER TABLE t_payment_detail DROP COLUMN IF EXISTS detail_name");
        jdbc.execute("ALTER TABLE t_payment_detail DROP COLUMN IF EXISTS detail_name_en");
        jdbc.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint
                        WHERE conname = 'fk_t_payment_detail_item'
                    ) THEN
                        ALTER TABLE t_payment_detail
                        ADD CONSTRAINT fk_t_payment_detail_item
                        FOREIGN KEY ("ID_payment_item")
                        REFERENCES t_payment_item("ID_payment_item");
                    END IF;
                END $$
                """);
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_t_reciept_rec_no ON t_reciept(rec_no)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_reciept_payment ON t_reciept(\"ID_payment\")");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_payment_reciept ON t_payment(\"ID_reciept\")");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_payment_monthly_bill ON t_payment(ID_monthly_rent_bill)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_payment_booking ON t_payment(\"ID_booking\")");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_payment_deposit_refund ON t_payment(\"ID_deposit_refund\")");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_payment_detail_payment ON t_payment_detail(\"ID_payment\")");
        jdbc.execute("CREATE INDEX IF NOT EXISTS ix_t_payment_detail_item ON t_payment_detail(\"ID_payment_item\")");
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_t_payment_item_name ON t_payment_item(item_name)");
        jdbc.update("""
                INSERT INTO t_payment_item (item_name, item_name_en)
                VALUES
                    ('ค่าเช่ารายเดือน', 'Monthly rent'),
                    ('ค่าห้องรายวัน', 'Daily room charge'),
                    ('ค่าน้ำ', 'Water charge'),
                    ('ค่าไฟ', 'Electricity charge'),
                    ('ค่าอื่น ๆ', 'Other charge'),
                    ('ส่วนลด', 'Discount'),
                    ('หักเงินล่วงหน้า', 'Advance payment deduction'),
                    ('ค่าประกัน', 'Deposit'),
                    ('หักค่าประกัน', 'Deposit deduction'),
                    ('มัดจำจองห้อง', 'Booking deposit'),
                    ('หักมัดจำจอง', 'Booking deposit deduction'),
                    ('ค่าบริการของลูกค้ารายวัน', 'Daily guest service'),
                    ('ค่าปรับ', 'Penalty'),
                    ('ค่าปรับเช็กเอาต์ล่าช้า', 'Late check-out penalty'),
                    ('ค่าเสียหาย', 'Damage charge')
                ON CONFLICT (item_name) DO NOTHING
                """);
        jdbc.update("""
                INSERT INTO t_reciept_type ("ID_rec_type", rec_type_name)
                VALUES
                    (1, 'ค่าแรกเข้า รายเดือน'),
                    (2, 'ค่าบริการของลูกค้ารายวัน'),
                    (3, 'ค่าเช่ารายเดือน'),
                    (4, 'มัดจำจองห้อง'),
                    (5, 'ค่าปรับ')
                ON CONFLICT ("ID_rec_type") DO UPDATE SET rec_type_name = EXCLUDED.rec_type_name
                """);
        boolean hasPaymentReceiptNo = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_name = 't_payment'
                      AND column_name = 'p_receipt_no'
                )
                """, Boolean.class);
        if (hasPaymentReceiptNo) {
            jdbc.update("""
                    UPDATE t_reciept r
                    SET rec_no = NULLIF(p.p_receipt_no, '')
                    FROM t_payment p
                    WHERE p."ID_reciept" = r."ID_reciept"
                      AND (r.rec_no IS NULL OR r.rec_no = '')
                      AND p.p_receipt_no IS NOT NULL
                      AND p.p_receipt_no <> ''
                    """);
            jdbc.update("""
                    INSERT INTO t_reciept (rec_date, rec_no, "ID_payment", "ID_rec_type", rec_amount)
                    SELECT
                        COALESCE(p.p_payment_date, CURRENT_DATE),
                        NULLIF(p.p_receipt_no, ''),
                        p."ID_payment",
                        CASE
                            WHEN p.p_remark LIKE 'ชำระบิลค่าเช่ารายเดือน%' THEN 3
                            WHEN p.p_remark LIKE '%#%' THEN 3
                            WHEN p.p_remark = 'ค่าแรกเข้า' AND g.g_stay_type = 'MONTHLY' THEN 1
                            WHEN p.p_remark = 'ค่าแรกเข้า' AND (g.g_stay_type = 'DAILY' OR g.g_stay_type IS NULL) THEN 2
                            WHEN g.g_stay_type = 'MONTHLY' THEN 1
                            ELSE 2
                        END,
                        COALESCE(p.p_amount, 0) + COALESCE(p.p_fine_amount, 0)
                    FROM t_payment p
                    LEFT JOIN t_guest g ON g."ID_guest" = p."ID_guest"
                    WHERE p.p_status = 'PAID'
                      AND COALESCE(p.p_amount, 0) + COALESCE(p.p_fine_amount, 0) > 0
                      AND NOT EXISTS (SELECT 1 FROM t_reciept)
                    """);
        } else {
            jdbc.update("""
                    INSERT INTO t_reciept (rec_date, rec_no, "ID_payment", "ID_rec_type", rec_amount)
                    SELECT
                        COALESCE(p.p_payment_date, CURRENT_DATE),
                        NULL,
                        p."ID_payment",
                        CASE
                            WHEN p.p_remark LIKE 'ชำระบิลค่าเช่ารายเดือน%' THEN 3
                            WHEN p.p_remark LIKE '%#%' THEN 3
                            WHEN p.p_remark = 'ค่าแรกเข้า' AND g.g_stay_type = 'MONTHLY' THEN 1
                            WHEN p.p_remark = 'ค่าแรกเข้า' AND (g.g_stay_type = 'DAILY' OR g.g_stay_type IS NULL) THEN 2
                            WHEN g.g_stay_type = 'MONTHLY' THEN 1
                            ELSE 2
                        END,
                        COALESCE(p.p_amount, 0) + COALESCE(p.p_fine_amount, 0)
                    FROM t_payment p
                    LEFT JOIN t_guest g ON g."ID_guest" = p."ID_guest"
                    WHERE p.p_status = 'PAID'
                      AND COALESCE(p.p_amount, 0) + COALESCE(p.p_fine_amount, 0) > 0
                      AND NOT EXISTS (SELECT 1 FROM t_reciept)
                    """);
        }
        jdbc.update("""
                UPDATE t_reciept r
                SET rec_no = numbered.rec_no
                FROM (
                    SELECT
                        "ID_reciept",
                        'P' || to_char(COALESCE(rec_date, CURRENT_DATE), 'YYYY') ||
                        lpad(row_number() OVER (
                            PARTITION BY to_char(COALESCE(rec_date, CURRENT_DATE), 'YYYY')
                            ORDER BY "ID_reciept"
                        )::text, 6, '0') AS rec_no
                    FROM t_reciept
                    WHERE rec_no IS NULL OR rec_no = ''
                ) numbered
                WHERE r."ID_reciept" = numbered."ID_reciept"
                """);
        jdbc.update("""
                UPDATE t_reciept r
                SET "ID_payment" = matched."ID_payment"
                FROM (
                    SELECT r2."ID_reciept", p."ID_payment"
                    FROM t_reciept r2
                    JOIN t_payment p
                      ON p.p_status = 'PAID'
                     AND COALESCE(p.p_amount, 0) + COALESCE(p.p_fine_amount, 0) = r2.rec_amount
                     AND COALESCE(p.p_payment_date, CURRENT_DATE) = r2.rec_date
                     AND (r2."ID_payment" IS NULL OR p."ID_payment" = r2."ID_payment")
                    WHERE r2."ID_payment" IS NULL
                ) matched
                WHERE r."ID_reciept" = matched."ID_reciept"
                """);
        jdbc.update("""
                UPDATE t_payment p
                SET "ID_reciept" = matched."ID_reciept"
                FROM (
                    SELECT DISTINCT ON (r."ID_payment")
                        r."ID_payment",
                        r."ID_reciept"
                    FROM t_reciept r
                    WHERE r."ID_payment" IS NOT NULL
                    ORDER BY r."ID_payment", r."ID_reciept"
                ) matched
                WHERE p."ID_payment" = matched."ID_payment"
                  AND p."ID_reciept" IS NULL
                """);
        jdbc.update("""
                UPDATE t_payment p
                SET ID_monthly_rent_bill = b.id_monthly_rent_bill
                FROM t_monthly_rent_bill b
                WHERE p.ID_monthly_rent_bill IS NULL
                  AND p.p_remark LIKE concat('%#', b.bill_number, '%')
                """);
        jdbc.update("""
                WITH paid_monthly AS (
                    SELECT
                        p."ID_payment",
                        row_number() OVER (ORDER BY substring(p.p_remark from '#([^\\s-]+)'), p."ID_payment") AS seq
                    FROM t_payment p
                    JOIN t_reciept r ON r."ID_reciept" = p."ID_reciept"
                    WHERE p.ID_monthly_rent_bill IS NULL
                      AND r."ID_rec_type" = 3
                      AND p.p_remark LIKE '%#%'
                ),
                monthly_map AS (
                    SELECT
                        b.id_monthly_rent_bill,
                        row_number() OVER (ORDER BY b.bill_number) AS seq
                    FROM t_monthly_rent_bill b
                    WHERE b.bill_number IS NOT NULL AND b.bill_number <> ''
                )
                UPDATE t_payment p
                SET ID_monthly_rent_bill = monthly_map.id_monthly_rent_bill
                FROM paid_monthly
                JOIN monthly_map ON monthly_map.seq = paid_monthly.seq
                WHERE p."ID_payment" = paid_monthly."ID_payment"
                """);
        jdbc.execute("ALTER TABLE t_payment DROP COLUMN IF EXISTS p_payment_type");
        jdbc.execute("ALTER TABLE t_payment DROP COLUMN IF EXISTS p_receipt_no");
    }
}
