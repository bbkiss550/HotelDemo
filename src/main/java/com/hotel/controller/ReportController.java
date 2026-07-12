package com.hotel.controller;

import com.hotel.service.JasperReportPdfService;
import com.hotel.service.ReportDataService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import net.sf.jasperreports.engine.JRException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/reports")
public class ReportController {
    private final ReportDataService reportData;
    private final JasperReportPdfService jasperReports;

    public ReportController(ReportDataService reportData, JasperReportPdfService jasperReports) {
        this.reportData = reportData;
        this.jasperReports = jasperReports;
    }

    @GetMapping
    String index() {
        return "redirect:/reports/revenue";
    }

    @GetMapping("/revenue")
    String revenue(@RequestParam(required = false) LocalDate startDate,
                   @RequestParam(required = false) LocalDate endDate,
                   Model model) {
        var range = addDateRange(model, startDate, endDate, "/reports/revenue");
        var report = reportData.revenue(range);
        model.addAttribute("reportType", "revenue");
        model.addAttribute("payments", report.payments());
        model.addAttribute("paymentPayerNames", report.payerNames());
        model.addAttribute("paymentReceiptNumbers", report.receiptNumbers());
        model.addAttribute("totalRevenue", report.totalRevenue());
        model.addAttribute("fineRevenue", report.fineRevenue());
        model.addAttribute("revenueByType", report.revenueByType());
        model.addAttribute("revenueByMethod", report.revenueByMethod());
        return "reports/revenue";
    }

    @GetMapping("/monthly-bills")
    String monthlyBills(@RequestParam(required = false) LocalDate startDate,
                        @RequestParam(required = false) LocalDate endDate,
                        Model model) {
        var range = addDateRange(model, startDate, endDate, "/reports/monthly-bills");
        var report = reportData.monthlyBills(range);
        model.addAttribute("reportType", "monthly-bills");
        model.addAttribute("monthlyBills", report.monthlyBills());
        model.addAttribute("outstandingAmount", report.outstandingAmount());
        return "reports/detail";
    }

    @GetMapping("/rooms")
    String rooms(Model model) {
        var report = reportData.rooms();
        model.addAttribute("reportType", "rooms");
        model.addAttribute("reportPdfAction", "/reports/rooms.pdf");
        model.addAttribute("totalRooms", report.totalRooms());
        model.addAttribute("availableRooms", report.availableRooms());
        model.addAttribute("occupiedRooms", report.occupiedRooms());
        model.addAttribute("roomStatusCounts", report.roomStatusCounts());
        return "reports/detail";
    }

    @GetMapping("/bookings")
    String bookings(@RequestParam(required = false) LocalDate startDate,
                    @RequestParam(required = false) LocalDate endDate,
                    Model model) {
        var range = addDateRange(model, startDate, endDate, "/reports/bookings");
        var report = reportData.bookings(range);
        model.addAttribute("reportType", "bookings");
        model.addAttribute("bookings", report.bookings());
        return "reports/detail";
    }

    @GetMapping("/deposit-refunds")
    String depositRefunds(@RequestParam(required = false) LocalDate startDate,
                          @RequestParam(required = false) LocalDate endDate,
                          Model model) {
        var range = addDateRange(model, startDate, endDate, "/reports/deposit-refunds");
        var report = reportData.depositRefunds(range);
        model.addAttribute("reportType", "deposit-refunds");
        model.addAttribute("depositRefunds", report.depositRefunds());
        model.addAttribute("refundAmount", report.refundAmount());
        return "reports/detail";
    }

    @GetMapping("/revenue.pdf")
    void revenuePdf(@RequestParam(required = false) LocalDate startDate,
                    @RequestParam(required = false) LocalDate endDate,
                    HttpServletResponse response) throws IOException, JRException {
        var range = reportData.range(startDate, endDate);
        writePdf(response, "revenue-report.pdf", jasperReports.revenuePdf(range, reportData.revenue(range)));
    }

    @GetMapping("/monthly-bills.pdf")
    void monthlyBillsPdf(@RequestParam(required = false) LocalDate startDate,
                         @RequestParam(required = false) LocalDate endDate,
                         HttpServletResponse response) throws IOException, JRException {
        var range = reportData.range(startDate, endDate);
        writePdf(response, "monthly-bills-report.pdf", jasperReports.monthlyBillsPdf(range, reportData.monthlyBills(range)));
    }

    @GetMapping("/rooms.pdf")
    void roomsPdf(HttpServletResponse response) throws IOException, JRException {
        writePdf(response, "rooms-report.pdf", jasperReports.roomsPdf(reportData.rooms()));
    }

    @GetMapping("/bookings.pdf")
    void bookingsPdf(@RequestParam(required = false) LocalDate startDate,
                     @RequestParam(required = false) LocalDate endDate,
                     HttpServletResponse response) throws IOException, JRException {
        var range = reportData.range(startDate, endDate);
        writePdf(response, "bookings-report.pdf", jasperReports.bookingsPdf(range, reportData.bookings(range)));
    }

    @GetMapping("/deposit-refunds.pdf")
    void depositRefundsPdf(@RequestParam(required = false) LocalDate startDate,
                           @RequestParam(required = false) LocalDate endDate,
                           HttpServletResponse response) throws IOException, JRException {
        var range = reportData.range(startDate, endDate);
        writePdf(response, "deposit-refunds-report.pdf", jasperReports.depositRefundsPdf(range, reportData.depositRefunds(range)));
    }

    private ReportDataService.DateRange addDateRange(Model model, LocalDate startDate, LocalDate endDate, String actionPath) {
        var range = reportData.range(startDate, endDate);
        model.addAttribute("startDate", range.start());
        model.addAttribute("endDate", range.end());
        model.addAttribute("reportAction", actionPath);
        model.addAttribute("reportPdfAction", actionPath + ".pdf");
        return range;
    }

    private void writePdf(HttpServletResponse response, String filename, byte[] pdf) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }
}
