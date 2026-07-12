package com.hotel.service;

import com.hotel.model.Payment;
import java.time.LocalDate;
import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.VerticalTextAlignEnum;
import org.springframework.stereotype.Service;

@Service
public class JasperReportPdfService {
    private static final int PAGE_WIDTH = 842;
    private static final int PAGE_HEIGHT = 595;
    private static final int MARGIN = 24;
    private static final int TABLE_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final String FONT_PATH = "C:/Windows/Fonts/tahoma.ttf";

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

    private byte[] tablePdf(String title, String subtitle, List<ReportColumn> columns, List<Map<String, ?>> rows) throws JRException {
        JasperDesign design = new JasperDesign();
        design.setName("hotel_report");
        design.setPageWidth(PAGE_WIDTH);
        design.setPageHeight(PAGE_HEIGHT);
        design.setLeftMargin(MARGIN);
        design.setRightMargin(MARGIN);
        design.setTopMargin(MARGIN);
        design.setBottomMargin(MARGIN);
        design.setColumnWidth(TABLE_WIDTH);

        JRDesignStyle baseStyle = new JRDesignStyle();
        baseStyle.setName("base");
        baseStyle.setDefault(true);
        baseStyle.setFontName("Tahoma");
        if (new File(FONT_PATH).exists()) {
            baseStyle.setPdfFontName(FONT_PATH);
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

        JRDesignBand titleBand = new JRDesignBand();
        titleBand.setHeight(62);
        titleBand.addElement(staticText(title, 0, 0, TABLE_WIDTH, 26, 18, true, HorizontalTextAlignEnum.LEFT, null));
        titleBand.addElement(staticText(subtitle, 0, 30, TABLE_WIDTH, 20, 10, false, HorizontalTextAlignEnum.LEFT, null));
        design.setTitle(titleBand);

        JRDesignBand headerBand = new JRDesignBand();
        headerBand.setHeight(24);
        int x = 0;
        for (ReportColumn column : columns) {
            headerBand.addElement(staticText(column.label(), x, 0, column.width(), 24, 10, true,
                    column.rightAlign() ? HorizontalTextAlignEnum.RIGHT : HorizontalTextAlignEnum.LEFT,
                    new Color(246, 249, 252)));
            x += column.width();
        }
        design.setColumnHeader(headerBand);

        JRDesignBand detailBand = new JRDesignBand();
        detailBand.setHeight(22);
        x = 0;
        for (ReportColumn column : columns) {
            detailBand.addElement(textField(column.field(), x, 0, column.width(), 22,
                    column.rightAlign() ? HorizontalTextAlignEnum.RIGHT : HorizontalTextAlignEnum.LEFT));
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

    private Map<String, ?> row(Object... values) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            row.put(String.valueOf(values[i]), values[i + 1]);
        }
        return row;
    }

    private record ReportColumn(String field, String label, int width, boolean rightAlign) {
    }
}
