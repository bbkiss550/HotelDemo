package com.hotel.service;

import com.hotel.model.Payment;
import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRLineBox;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignEllipse;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignRectangle;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import org.springframework.stereotype.Service;

@Service
public class JasperReportPdfService {
    private static final int PAGE_WIDTH = 842;
    private static final int PAGE_HEIGHT = 595;
    private static final int MARGIN = 24;
    private static final int TABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final String LINUX_THAI_FONT_PATH = "/usr/share/fonts/truetype/tlwg/Garuda.ttf";
    private static final String WINDOWS_THAI_FONT_PATH = "C:/Windows/Fonts/tahoma.ttf";

    private final ThaiDateFormatter thaiDate;

    public JasperReportPdfService(ThaiDateFormatter thaiDate) {
        this.thaiDate = thaiDate;
    }

    public byte[] revenuePdf(ReportDataService.DateRange range, ReportDataService.RevenueReport report) throws JRException {
        List<Map<String, ?>> rows = report.payments().stream()
                .<Map<String, ?>>map(payment -> row(
                        "receiptNo", report.receiptNumbers().getOrDefault(payment.getId(), "-"),
                        "receiptDate", payment.getReciept() == null ? "-" : thaiDate.format(payment.getReciept().getRecieptDate()),
                        "paymentDate", thaiDate.format(payment.getPaymentDate()),
                        "payerName", report.payerNames().getOrDefault(payment.getId(), "-"),
                        "receiptType", payment.getReciept() != null && payment.getReciept().getType() != null ? payment.getReciept().getType().getName() : "-",
                        "paymentMethod", payment.getPaymentMethod() == null || payment.getPaymentMethod().isBlank() ? "-" : payment.getPaymentMethod(),
                        "amount", money(payment.getAmount())
                ))
                .toList();
        return tablePdf(
                "รายงานรายได้",
                "วันที่บันทึก " + thaiDate.format(range.start()) + " - " + thaiDate.format(range.end()),
                List.of(
                        column("receiptNo", "เลขใบเสร็จ", 105, false),
                        column("receiptDate", "วันที่ใบเสร็จ", 90, false),
                        column("paymentDate", "วันที่จ่าย", 90, false),
                        column("payerName", "ผู้จ่าย", 170, false),
                        column("receiptType", "ประเภทใบเสร็จ", 150, false),
                        column("paymentMethod", "วิธีชำระ", 80, false),
                        column("amount", "ยอดตามใบแจ้ง", 105, true)
                ),
                rows
        );
    }

    public byte[] monthlyBillsPdf(ReportDataService.DateRange range, ReportDataService.MonthlyBillReport report) throws JRException {
        List<Map<String, ?>> rows = report.monthlyBills().stream()
                .<Map<String, ?>>map(bill -> row(
                        "billNo", value(bill.getDisplayBillNumber()),
                        "billingPeriod", monthYear(bill.getBillingMonth(), bill.getBillingYear()),
                        "roomNo", bill.getRoom() == null ? "-" : value(bill.getRoom().getRoomNumber()),
                        "guestName", bill.getGuest() == null ? "-" : value(bill.getGuest().getFullName()),
                        "totalAmount", money(bill.getTotalAmount()),
                        "remainingAmount", money(bill.getRemainingAmount()),
                        "dueDate", date(bill.getDueDate()),
                        "status", bill.getBillStatus() != null ? value(bill.getBillStatus().getName()) : bill.getStatus().getLabel()
                ))
                .toList();
        return tablePdf(
                "รายงานใบแจ้งค่าเช่า",
                "วันที่บันทึก " + thaiDate.format(range.start()) + " - " + thaiDate.format(range.end()),
                List.of(
                        column("billNo", "เลขบิล", 105, false),
                        column("billingPeriod", "รอบบิล", 95, false),
                        column("roomNo", "ห้อง", 60, false),
                        column("guestName", "ผู้พัก", 170, false),
                        column("totalAmount", "ยอดสุทธิ", 90, true),
                        column("remainingAmount", "คงเหลือ", 90, true),
                        column("dueDate", "ครบกำหนด", 90, false),
                        column("status", "สถานะ", 80, false)
                ),
                rows
        );
    }

    public byte[] roomsPdf(ReportDataService.RoomReport report) throws JRException {
        List<Map<String, ?>> rows = report.roomStatusCounts().entrySet().stream()
                .<Map<String, ?>>map(entry -> row("status", entry.getKey(), "roomCount", String.valueOf(entry.getValue())))
                .toList();
        return tablePdf(
                "รายงานสถานะห้องพัก",
                "ห้องทั้งหมด " + report.totalRooms() + " | ห้องว่าง " + report.availableRooms() + " | มีผู้พัก " + report.occupiedRooms(),
                List.of(
                        column("status", "สถานะ", 500, false),
                        column("roomCount", "จำนวนห้อง", 150, true)
                ),
                rows
        );
    }

    public byte[] bookingsPdf(ReportDataService.DateRange range, ReportDataService.BookingReport report) throws JRException {
        List<Map<String, ?>> rows = report.bookings().stream()
                .<Map<String, ?>>map(booking -> row(
                        "bookingNo", value(booking.getBookingNumber()),
                        "checkInDate", date(booking.getCheckInDate()),
                        "checkOutDate", date(booking.getCheckOutDate()),
                        "customerName", value(booking.getCustomerName()),
                        "stayType", booking.getStayType() == null ? "-" : booking.getStayType().getLabel(),
                        "depositAmount", money(booking.getDepositAmount()),
                        "status", booking.getStatus() == null ? "-" : booking.getStatus().getLabel()
                ))
                .toList();
        return tablePdf(
                "รายงานการจอง",
                "วันที่บันทึก " + thaiDate.format(range.start()) + " - " + thaiDate.format(range.end()),
                List.of(
                        column("bookingNo", "เลขที่จอง", 120, false),
                        column("checkInDate", "วันที่เข้า", 95, false),
                        column("checkOutDate", "วันที่ออก", 95, false),
                        column("customerName", "ผู้จอง", 210, false),
                        column("stayType", "ประเภท", 80, false),
                        column("depositAmount", "มัดจำ", 95, true),
                        column("status", "สถานะ", 95, false)
                ),
                rows
        );
    }

    public byte[] depositRefundsPdf(ReportDataService.DateRange range, ReportDataService.DepositRefundReport report) throws JRException {
        List<Map<String, ?>> rows = report.depositRefunds().stream()
                .<Map<String, ?>>map(refund -> row(
                        "refundNo", value(refund.getRefundNo()),
                        "refundDate", date(refund.getRefundDate()),
                        "roomNo", refund.getRoom() == null ? "-" : value(refund.getRoom().getRoomNumber()),
                        "receiverName", refund.getGuest() == null ? "-" : value(refund.getGuest().getFullName()),
                        "depositAmount", money(refund.getDepositAmount()),
                        "deductAmount", money(refund.getTotalDeductAmount()),
                        "refundAmount", money(refund.getRefundAmount()),
                        "refundMethod", value(refund.getRefundMethod())
                ))
                .toList();
        return tablePdf(
                "รายงานคืนเงินประกัน",
                "วันที่บันทึก " + thaiDate.format(range.start()) + " - " + thaiDate.format(range.end()),
                List.of(
                        column("refundNo", "เลขเอกสาร", 110, false),
                        column("refundDate", "วันที่", 80, false),
                        column("roomNo", "ห้อง", 55, false),
                        column("receiverName", "ผู้รับเงิน", 180, false),
                        column("depositAmount", "ค่าประกัน", 90, true),
                        column("deductAmount", "หักรวม", 85, true),
                        column("refundAmount", "คืนสุทธิ", 85, true),
                        column("refundMethod", "วิธีคืน", 70, false)
                ),
                rows
        );
    }

    private byte[] revenueDashboardPdf(String title, String subtitle, List<ReportColumn> columns,
                                       List<Map<String, ?>> rows) throws JRException {
        JasperDesign design = reportDesign("revenue_dashboard", columns);
        FontConfig font = fontConfig();

        JRDesignBand header = new JRDesignBand();
        header.setHeight(192);
        header.addElement(panel(0, 0, TABLE_WIDTH, 110, new Color(250, 252, 255), new Color(218, 227, 240)));
        header.addElement(circle(24, 25, 58, new Color(22, 79, 144)));
        JRDesignStaticText icon = staticText("฿", 24, 25, 58, 58, 27, true, HorizontalTextAlignEnum.CENTER, null);
        icon.setForecolor(Color.WHITE);
        header.addElement(icon);

        JRDesignStaticText reportTitle = staticText(title, 98, 22, 440, 42, 24, true,
                HorizontalTextAlignEnum.LEFT, null);
        applyPdfFont(reportTitle, font);
        reportTitle.setForecolor(new Color(19, 52, 101));
        header.addElement(reportTitle);
        JRDesignStaticText reportCondition = staticText(subtitle, 98, 66, 450, 22, 12, false,
                HorizontalTextAlignEnum.LEFT, null);
        reportCondition.setForecolor(new Color(76, 98, 132));
        header.addElement(reportCondition);

        header.addElement(panel(620, 28, 150, 60, new Color(238, 244, 253), new Color(222, 231, 244)));
        JRDesignStaticText printedLabel = staticText("พิมพ์เมื่อ", 664, 36, 92, 18, 10, true,
                HorizontalTextAlignEnum.LEFT, null);
        printedLabel.setForecolor(new Color(48, 70, 107));
        header.addElement(printedLabel);
        JRDesignStaticText printedValue = staticText(printedAt().replace("พิมพ์เมื่อ ", ""), 664, 55, 94, 22, 9, false,
                HorizontalTextAlignEnum.LEFT, null);
        printedValue.setForecolor(new Color(25, 51, 94));
        header.addElement(printedValue);

        BigDecimal total = rows.stream()
                .map(row -> decimalValue(row.get("amount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = rows.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP);
        header.addElement(panel(0, 132, TABLE_WIDTH, 60, Color.WHITE, new Color(218, 227, 240)));
        addMetric(header, 22, "ยอดรวมรายได้ทั้งสิ้น", money(total), "บาท", "฿", true);
        header.addElement(divider(270, 146, 1, 34));
        addMetric(header, 306, "จำนวนรายการ", rows.size() + " รายการ", "", "#", false);
        header.addElement(divider(512, 146, 1, 34));
        addMetric(header, 546, "ยอดเฉลี่ยต่อรายการ", money(average), "บาท", "avg", false);
        design.setPageHeader(header);

        List<ReportColumn> dashboardColumns = List.of(
                column("rowNo", "ลำดับ", 45, false),
                column("receiptNo", "เลขใบเสร็จ", 100, false),
                column("receiptDate", "วันที่ใบเสร็จ", 85, false),
                column("paymentDate", "วันที่จ่าย", 85, false),
                column("payerName", "ผู้จ่าย", 140, false),
                column("receiptType", "ประเภทใบเสร็จ", 140, false),
                column("paymentMethod", "วิธีชำระ", 85, false),
                column("amount", "ยอดตามใบแจ้ง", 114, true)
        );
        JRDesignBand columnHeader = new JRDesignBand();
        columnHeader.setHeight(34);
        int x = 0;
        for (ReportColumn column : dashboardColumns) {
            JRDesignStaticText cell = staticText(column.label(), x, 0, column.width(), 34, 10, true,
                    HorizontalTextAlignEnum.CENTER, new Color(24, 82, 145));
            cell.setForecolor(Color.WHITE);
            dashboardBorder(cell.getLineBox(), Color.WHITE, 0.35f, 7);
            columnHeader.addElement(cell);
            x += column.width();
        }
        design.setColumnHeader(columnHeader);

        JRDesignBand detail = new JRDesignBand();
        detail.setHeight(40);
        x = 0;
        for (ReportColumn column : dashboardColumns) {
            JRDesignTextField cell = "rowNo".equals(column.field())
                    ? textExpression("$V{REPORT_COUNT}", x, 0, column.width(), 40, HorizontalTextAlignEnum.CENTER)
                    : textField(column.field(), x, 0, column.width(), 40,
                            column.rightAlign() ? HorizontalTextAlignEnum.RIGHT : HorizontalTextAlignEnum.CENTER);
            if ("amount".equals(column.field())) {
                cell.setBold(true);
                cell.setForecolor(new Color(18, 60, 131));
            }
            cell.setMode(ModeEnum.OPAQUE);
            cell.setBackcolor(Color.WHITE);
            dashboardBorder(cell.getLineBox(), new Color(222, 229, 238), 0.45f, 7);
            detail.addElement(cell);
            x += column.width();
        }
        ((JRDesignSection) design.getDetailSection()).addBand(detail);

        JRDesignBand footer = new JRDesignBand();
        footer.setHeight(38);
        footer.addElement(divider(0, 2, TABLE_WIDTH, 1));
        JRDesignStaticText note = staticText("หมายเหตุ: ยอดรายได้รวมตามใบแจ้ง", 18, 12, 360, 18, 9, false,
                HorizontalTextAlignEnum.LEFT, null);
        note.setForecolor(new Color(73, 91, 118));
        footer.addElement(note);
        JRDesignTextField page = textExpression("\"หน้า \" + $V{PAGE_NUMBER}", TABLE_WIDTH - 90, 12, 90, 18,
                HorizontalTextAlignEnum.RIGHT);
        page.setForecolor(new Color(73, 91, 118));
        footer.addElement(page);
        design.setPageFooter(footer);

        JasperPrint print = JasperFillManager.fillReport(
                JasperCompileManager.compileReport(design), new HashMap<>(), new JRMapCollectionDataSource(new ArrayList<>(rows)));
        return JasperExportManager.exportReportToPdf(print);
    }

    private byte[] tablePdf(String title, String subtitle, List<ReportColumn> columns, List<Map<String, ?>> rows) throws JRException {
        if (!columns.isEmpty() && "receiptNo".equals(columns.get(0).field())) {
            return revenueDashboardPdf(title, subtitle, columns, rows);
        }
        JasperDesign design = new JasperDesign();
        design.setName("hotel_report");
        design.setPageWidth(PAGE_WIDTH);
        design.setPageHeight(PAGE_HEIGHT);
        design.setOrientation(OrientationEnum.LANDSCAPE);
        design.setLeftMargin(MARGIN);
        design.setRightMargin(MARGIN);
        design.setTopMargin(MARGIN);
        design.setBottomMargin(MARGIN);
        design.setColumnWidth(TABLE_WIDTH);

        JRDesignStyle baseStyle = new JRDesignStyle();
        baseStyle.setName("base");
        baseStyle.setDefault(true);
        FontConfig font = fontConfig();
        baseStyle.setFontName(font.name());
        if (font.path() != null) {
            baseStyle.setPdfFontName(font.path());
            baseStyle.setPdfEncoding("Identity-H");
            baseStyle.setPdfEmbedded(true);
        }
        baseStyle.setFontSize(10f);
        design.addStyle(baseStyle);

        columns.forEach(column -> {
            JRDesignField field = new JRDesignField();
            field.setName(column.field());
            field.setValueClass(String.class);
            try {
                design.addField(field);
            } catch (JRException e) {
                throw new IllegalStateException(e);
            }
        });

        JRDesignBand reportHeader = new JRDesignBand();
        reportHeader.setHeight(112);
        JRDesignStaticText reportTitle = staticText(title, 0, 4, TABLE_WIDTH, 46, 18, true,
                HorizontalTextAlignEnum.CENTER, null);
        reportHeader.addElement(reportTitle);
        JRDesignStaticText reportRange = staticText(subtitle, 0, 58, TABLE_WIDTH, 22, 10, false,
                HorizontalTextAlignEnum.CENTER, null);
        reportHeader.addElement(reportRange);
        JRDesignStaticText reportPrintedAt = staticText(printedAt(), 0, 84, TABLE_WIDTH, 18, 9, false,
                HorizontalTextAlignEnum.RIGHT, null);
        reportHeader.addElement(reportPrintedAt);
        design.setPageHeader(reportHeader);

        JRDesignBand headerBand = new JRDesignBand();
        headerBand.setHeight(30);
        int x = 0;
        for (ReportColumn column : columns) {
            JRDesignStaticText headerCell = staticText(column.label(), x, 0, column.width(), 30, 10, true,
                    HorizontalTextAlignEnum.CENTER, new Color(235, 236, 238));
            applyTableBorder(headerCell);
            headerBand.addElement(headerCell);
            x += column.width();
        }
        design.setColumnHeader(headerBand);

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(28);
        x = 0;
        for (ReportColumn column : columns) {
            JRDesignTextField detailCell = textField(column.field(), x, 0, column.width(), 28,
                    column.rightAlign() ? HorizontalTextAlignEnum.RIGHT : HorizontalTextAlignEnum.LEFT);
            applyTableBorder(detailCell);
            detailBand.addElement(detailCell);
            x += column.width();
        }
        ((JRDesignSection) design.getDetailSection()).addBand(detailBand);

        JRDesignBand summaryBand = new JRDesignBand();
        summaryBand.setHeight(20);
        summaryBand.addElement(staticText("รวม " + rows.size() + " รายการ", 0, 0, TABLE_WIDTH, 18, 10, false, HorizontalTextAlignEnum.RIGHT, null));
        design.setSummary(summaryBand);

        Map<String, Object> parameters = new HashMap<>();
        JasperPrint print = JasperFillManager.fillReport(
                JasperCompileManager.compileReport(design),
                parameters,
                new JRMapCollectionDataSource(new ArrayList<>(rows))
        );
        return JasperExportManager.exportReportToPdf(print);
    }

    private JasperDesign reportDesign(String name, List<ReportColumn> columns) {
        JasperDesign design = new JasperDesign();
        design.setName(name);
        design.setPageWidth(PAGE_WIDTH);
        design.setPageHeight(PAGE_HEIGHT);
        design.setOrientation(OrientationEnum.LANDSCAPE);
        design.setLeftMargin(MARGIN);
        design.setRightMargin(MARGIN);
        design.setTopMargin(MARGIN);
        design.setBottomMargin(MARGIN);
        design.setColumnWidth(TABLE_WIDTH);

        FontConfig font = fontConfig();
        JRDesignStyle baseStyle = new JRDesignStyle();
        baseStyle.setName("base");
        baseStyle.setDefault(true);
        baseStyle.setFontName(font.name());
        baseStyle.setFontSize(10f);
        if (font.path() != null) {
            baseStyle.setPdfFontName(font.path());
            baseStyle.setPdfEncoding("Identity-H");
            baseStyle.setPdfEmbedded(true);
        }
        try {
            design.addStyle(baseStyle);
            for (ReportColumn column : columns) {
                JRDesignField field = new JRDesignField();
                field.setName(column.field());
                field.setValueClass(String.class);
                design.addField(field);
            }
        } catch (JRException ex) {
            throw new IllegalStateException(ex);
        }
        return design;
    }

    private JRDesignStaticText panel(int x, int y, int width, int height, Color background, Color border) {
        JRDesignStaticText element = staticText("", x, y, width, height, 1, false,
                HorizontalTextAlignEnum.LEFT, background);
        dashboardBorder(element.getLineBox(), border, 0.45f, 0);
        return element;
    }

    private JRDesignEllipse circle(int x, int y, int size, Color background) {
        JRDesignEllipse element = new JRDesignEllipse(null);
        element.setX(x);
        element.setY(y);
        element.setWidth(size);
        element.setHeight(size);
        element.setMode(ModeEnum.OPAQUE);
        element.setBackcolor(background);
        return element;
    }

    private JRDesignRectangle divider(int x, int y, int width, int height) {
        JRDesignRectangle element = new JRDesignRectangle();
        element.setX(x);
        element.setY(y);
        element.setWidth(width);
        element.setHeight(height);
        element.setMode(ModeEnum.OPAQUE);
        element.setBackcolor(new Color(208, 220, 238));
        return element;
    }

    private void addMetric(JRDesignBand band, int x, String label, String value, String suffix, String symbol, boolean emphasized) {
        band.addElement(circle(x, 145, 34, new Color(237, 243, 252)));
        JRDesignStaticText icon = staticText(symbol, x, 145, 34, 34, emphasized ? 18 : 10, true,
                HorizontalTextAlignEnum.CENTER, null);
        icon.setForecolor(new Color(24, 82, 145));
        band.addElement(icon);
        JRDesignStaticText metricLabel = staticText(label, x + 46, 142, 188, 17, 9, false,
                HorizontalTextAlignEnum.LEFT, null);
        metricLabel.setForecolor(new Color(65, 88, 121));
        band.addElement(metricLabel);
        JRDesignStaticText metricValue = staticText(value, x + 46, 160, 142, 24, emphasized ? 18 : 12, true,
                HorizontalTextAlignEnum.LEFT, null);
        metricValue.setForecolor(new Color(16, 70, 146));
        band.addElement(metricValue);
        if (!suffix.isBlank()) {
            JRDesignStaticText metricSuffix = staticText(suffix, x + 190, 164, 30, 18, 9, false,
                    HorizontalTextAlignEnum.LEFT, null);
            metricSuffix.setForecolor(new Color(65, 88, 121));
            band.addElement(metricSuffix);
        }
    }

    private JRDesignTextField textExpression(String expression, int x, int y, int width, int height,
                                              HorizontalTextAlignEnum align) {
        JRDesignTextField element = new JRDesignTextField();
        element.setX(x);
        element.setY(y);
        element.setWidth(width);
        element.setHeight(height);
        element.setFontSize(9f);
        element.setHorizontalTextAlign(align);
        element.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        element.setExpression(new JRDesignExpression(expression));
        return element;
    }

    private void applyPdfFont(JRDesignStaticText element, FontConfig font) {
        element.setFontName(font.name());
        if (font.path() != null) {
            element.setPdfFontName(font.path());
            element.setPdfEncoding("Identity-H");
            element.setPdfEmbedded(true);
        }
    }

    private void dashboardBorder(JRLineBox lineBox, Color color, float width, int padding) {
        lineBox.getTopPen().setLineWidth(width);
        lineBox.getRightPen().setLineWidth(width);
        lineBox.getBottomPen().setLineWidth(width);
        lineBox.getLeftPen().setLineWidth(width);
        lineBox.getTopPen().setLineColor(color);
        lineBox.getRightPen().setLineColor(color);
        lineBox.getBottomPen().setLineColor(color);
        lineBox.getLeftPen().setLineColor(color);
        if (padding > 0) {
            lineBox.setTopPadding(padding);
            lineBox.setRightPadding(padding);
            lineBox.setBottomPadding(padding);
            lineBox.setLeftPadding(padding);
        }
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(String.valueOf(value).replace(",", ""));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private JRDesignStaticText staticText(String text, int x, int y, int width, int height, int fontSize, boolean bold,
                                          HorizontalTextAlignEnum align, Color backColor) {
        JRDesignStaticText element = new JRDesignStaticText();
        element.setX(x);
        element.setY(y);
        element.setWidth(width);
        element.setHeight(height);
        element.setText(text);
        element.setFontSize((float) fontSize);
        element.setBold(bold);
        element.setHorizontalTextAlign(align);
        element.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        if (backColor != null) {
            element.setMode(ModeEnum.OPAQUE);
            element.setBackcolor(backColor);
        }
        return element;
    }

    private JRDesignTextField textField(String field, int x, int y, int width, int height, HorizontalTextAlignEnum align) {
        JRDesignTextField element = new JRDesignTextField();
        element.setX(x);
        element.setY(y);
        element.setWidth(width);
        element.setHeight(height);
        element.setFontSize(9f);
        element.setHorizontalTextAlign(align);
        element.setVerticalTextAlign(VerticalTextAlignEnum.MIDDLE);
        element.setExpression(new JRDesignExpression("$F{" + field + "}"));
        return element;
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : String.format("%,.2f", value);
    }

    private String date(LocalDate value) {
        return value == null ? "-" : thaiDate.format(value);
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String monthYear(Integer month, Integer year) {
        if (month == null || year == null) {
            return "-";
        }
        return thaiDate.monthYear(month, year);
    }

    private ReportColumn column(String field, String label, int width, boolean rightAlign) {
        return new ReportColumn(field, label, width, rightAlign);
    }

    private FontConfig fontConfig() {
        if (new File(LINUX_THAI_FONT_PATH).isFile()) {
            return new FontConfig("Garuda", LINUX_THAI_FONT_PATH);
        }
        if (new File(WINDOWS_THAI_FONT_PATH).isFile()) {
            return new FontConfig("Tahoma", WINDOWS_THAI_FONT_PATH);
        }
        return new FontConfig("SansSerif", null);
    }

    private String printedAt() {
        return "พิมพ์เมื่อ " + thaiDate.format(LocalDate.now()) + " เวลา "
                + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + " น.";
    }

    private void applyTableBorder(JRDesignStaticText element) {
        applyTableBorder(element.getLineBox());
    }

    private void applyTableBorder(JRDesignTextField element) {
        applyTableBorder(element.getLineBox());
    }

    private void applyTableBorder(JRLineBox lineBox) {
        lineBox.getTopPen().setLineWidth(1f);
        lineBox.getRightPen().setLineWidth(1f);
        lineBox.getBottomPen().setLineWidth(1f);
        lineBox.getLeftPen().setLineWidth(1f);
        lineBox.getTopPen().setLineColor(Color.BLACK);
        lineBox.getRightPen().setLineColor(Color.BLACK);
        lineBox.getBottomPen().setLineColor(Color.BLACK);
        lineBox.getLeftPen().setLineColor(Color.BLACK);
        lineBox.setTopPadding(5);
        lineBox.setRightPadding(5);
        lineBox.setBottomPadding(5);
        lineBox.setLeftPadding(5);
    }

    private Map<String, ?> row(Object... values) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            row.put(String.valueOf(values[i]), values[i + 1]);
        }
        return row;
    }

    private record ReportColumn(String field, String label, int width, boolean rightAlign) {
    }

    private record FontConfig(String name, String path) {
    }
}
