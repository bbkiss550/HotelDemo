package com.hotel.config;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReportMenuInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public ReportMenuInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long parentId = findReportParentId();
        if (parentId == null) {
            jdbc.update("""
                    INSERT INTO t_menu (m_icon, m_name, m_link, m_sort_order, "ID_parent_menu")
                    VALUES ('bi-bar-chart-line', 'รายงาน', NULL, 80, NULL)
                    """);
            parentId = findReportParentId();
        }
        if (parentId == null) {
            return;
        }

        jdbc.update("""
                UPDATE t_menu
                SET m_icon = 'bi-bar-chart-line',
                    m_name = 'รายงาน',
                    m_link = NULL
                WHERE "ID_menu" = ?
                """, parentId);

        upsertReportMenu(parentId, "รายงานรายได้", "/reports/revenue", 10);
        upsertReportMenu(parentId, "รายงานใบแจ้งค่าเช่า", "/reports/monthly-bills", 20);
        upsertReportMenu(parentId, "รายงานสถานะห้องพัก", "/reports/rooms", 30);
        upsertReportMenu(parentId, "รายงานการจอง", "/reports/bookings", 40);
        upsertReportMenu(parentId, "รายงานคืนเงินประกัน", "/reports/deposit-refunds", 50);
    }

    private Long findReportParentId() {
        List<Long> ids = jdbc.query("""
                SELECT "ID_menu"
                FROM t_menu
                WHERE m_link = '/reports' OR m_name = 'รายงาน'
                ORDER BY "ID_menu"
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong(1));
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void upsertReportMenu(Long parentId, String name, String link, int sortOrder) {
        jdbc.update("""
                INSERT INTO t_menu (m_icon, m_name, m_link, m_sort_order, "ID_parent_menu")
                VALUES ('bi-file-earmark-bar-graph', ?, ?, ?, ?)
                ON CONFLICT (m_link) DO UPDATE
                SET m_icon = EXCLUDED.m_icon,
                    m_name = EXCLUDED.m_name,
                    m_sort_order = EXCLUDED.m_sort_order,
                    "ID_parent_menu" = EXCLUDED."ID_parent_menu"
                """, name, link, sortOrder, parentId);
    }
}
