package com.hotel.controller;

import com.hotel.service.JasperReportPdfService;
import com.hotel.service.ReportDataService;
import com.hotel.repository.MonthlyRentBillRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import net.sf.jasperreports.engine.JRException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/reports")
public class ReportController {
    private final ReportDataService reportData;
    private final JasperReportPdfService jasperReports;
    private final MonthlyRentBillRepository monthlyRentBills;
    private final Map<UUID, PreviewFile> previewFiles = new ConcurrentHashMap<>();

    public ReportController(ReportDataService reportData,
                            JasperReportPdfService jasperReports,
                            MonthlyRentBillRepository monthlyRentBills) {
        this.reportData = reportData;
        this.jasperReports = jasperReports;
        this.monthlyRentBills = monthlyRentBills;
    }

    @GetMapping
    String index() {
        return "redirect:/reports/revenue";
    }

    @GetMapping("/revenue")
    String revenue(@RequestParam(required = false, defaultValue = "all") String filterType,
                   @RequestParam(required = false) Integer year,
                   @RequestParam(required = false) Integer month,
                   @RequestParam(required = false) LocalDate startDate,
                   @RequestParam(required = false) LocalDate endDate,
                   Model model) {
        var selection = selectReportDates(filterType, year, month, startDate, endDate);
        addReportFilterModel(model, selection, "/reports/revenue");
        var range = selection.range();
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
    String monthlyBills(@RequestParam(required = false, defaultValue = "all") String filterType,
                        @RequestParam(required = false) String period,
                        @RequestParam(required = false) Integer year,
                        Model model) {
        var selection = selectMonthlyBills(filterType, period, year);
        model.addAttribute("reportType", "monthly-bills");
        model.addAttribute("reportAction", "/reports/monthly-bills");
        model.addAttribute("reportPdfAction", "/reports/monthly-bills.pdf");
        model.addAttribute("filterType", selection.filterType());
        model.addAttribute("billingPeriod", selection.period());
        model.addAttribute("billingYear", selection.year());
        model.addAttribute("defaultBillingPeriod", YearMonth.now().toString());
        model.addAttribute("defaultBillingYear", LocalDate.now().getYear());
        model.addAttribute("billingPeriods", billingPeriodOptions());
        model.addAttribute("billingYears", billingYearOptions());
        model.addAttribute("monthlyBills", selection.report().monthlyBills());
        model.addAttribute("outstandingAmount", selection.report().outstandingAmount());
        return "reports/detail";
    }

    @GetMapping("/rooms")
    String rooms(Model model) {
        var report = reportData.rooms();
        model.addAttribute("reportType", "rooms");
        model.addAttribute("reportPdfAction", "/reports/rooms.pdf");
        model.addAttribute("reportPreviewAction", "/reports/rooms/preview");
        model.addAttribute("totalRooms", report.totalRooms());
        model.addAttribute("availableRooms", report.availableRooms());
        model.addAttribute("occupiedRooms", report.occupiedRooms());
        model.addAttribute("roomStatusCounts", report.roomStatusCounts());
        return "reports/detail";
    }

    @GetMapping("/bookings")
    String bookings(@RequestParam(required = false) LocalDate startDate,
                    @RequestParam(required = false) LocalDate endDate,
                    @RequestParam(required = false, defaultValue = "all") String filterType,
                    @RequestParam(required = false) Integer year,
                    @RequestParam(required = false) Integer month,
                    Model model) {
        var selection = selectReportDates(filterType, year, month, startDate, endDate);
        addReportFilterModel(model, selection, "/reports/bookings");
        var range = selection.range();
        var report = reportData.bookings(range);
        model.addAttribute("reportType", "bookings");
        model.addAttribute("bookings", report.bookings());
        return "reports/detail";
    }

    @GetMapping("/deposit-refunds")
    String depositRefunds(@RequestParam(required = false) LocalDate startDate,
                          @RequestParam(required = false) LocalDate endDate,
                          @RequestParam(required = false, defaultValue = "all") String filterType,
                          @RequestParam(required = false) Integer year,
                          @RequestParam(required = false) Integer month,
                          Model model) {
        var selection = selectReportDates(filterType, year, month, startDate, endDate);
        addReportFilterModel(model, selection, "/reports/deposit-refunds");
        var range = selection.range();
        var report = reportData.depositRefunds(range);
        model.addAttribute("reportType", "deposit-refunds");
        model.addAttribute("depositRefunds", report.depositRefunds());
        model.addAttribute("refundAmount", report.refundAmount());
        return "reports/detail";
    }

    @GetMapping("/revenue.pdf")
    void revenuePdf(@RequestParam(required = false) LocalDate startDate,
                    @RequestParam(required = false) LocalDate endDate,
                    @RequestParam(required = false, defaultValue = "all") String filterType,
                    @RequestParam(required = false) Integer year,
                    @RequestParam(required = false) Integer month,
                    @RequestParam(defaultValue = "false") boolean inline,
                    HttpServletResponse response) throws IOException, JRException {
        var selection = selectReportDates(filterType, year, month, startDate, endDate);
        var range = selection.range();
        writePdf(response, "revenue-report.pdf", jasperReports.revenuePdf(
                range, reportData.revenue(range), reportCondition(selection)), inline);
    }

    @GetMapping("/monthly-bills.pdf")
    void monthlyBillsPdf(@RequestParam(required = false, defaultValue = "all") String filterType,
                         @RequestParam(required = false) String period,
                         @RequestParam(required = false) Integer year,
                         @RequestParam(defaultValue = "false") boolean inline,
                         HttpServletResponse response) throws IOException, JRException {
        var selection = selectMonthlyBills(filterType, period, year);
        writePdf(response, "monthly-bills-report.pdf", jasperReports.monthlyBillsPdf(
                selection.range(), selection.report(), selection.showPeriodSummary(), selection.subtitle()), inline);
    }

    @GetMapping("/rooms.pdf")
    void roomsPdf(@RequestParam(defaultValue = "false") boolean inline, HttpServletResponse response) throws IOException, JRException {
        writePdf(response, "rooms-report.pdf", jasperReports.roomsPdf(reportData.rooms()), inline);
    }

    @GetMapping("/bookings.pdf")
    void bookingsPdf(@RequestParam(required = false) LocalDate startDate,
                     @RequestParam(required = false) LocalDate endDate,
                     @RequestParam(required = false, defaultValue = "all") String filterType,
                     @RequestParam(required = false) Integer year,
                     @RequestParam(required = false) Integer month,
                     @RequestParam(defaultValue = "false") boolean inline,
                     HttpServletResponse response) throws IOException, JRException {
        var selection = selectReportDates(filterType, year, month, startDate, endDate);
        var range = selection.range();
        writePdf(response, "bookings-report.pdf", jasperReports.bookingsPdf(
                range, reportData.bookings(range), reportCondition(selection)), inline);
    }

    @GetMapping("/deposit-refunds.pdf")
    void depositRefundsPdf(@RequestParam(required = false) LocalDate startDate,
                           @RequestParam(required = false) LocalDate endDate,
                           @RequestParam(required = false, defaultValue = "all") String filterType,
                           @RequestParam(required = false) Integer year,
                           @RequestParam(required = false) Integer month,
                           @RequestParam(defaultValue = "false") boolean inline,
                           HttpServletResponse response) throws IOException, JRException {
        var selection = selectReportDates(filterType, year, month, startDate, endDate);
        var range = selection.range();
        writePdf(response, "deposit-refunds-report.pdf", jasperReports.depositRefundsPdf(
                range, reportData.depositRefunds(range), reportCondition(selection)), inline);
    }

    @GetMapping("/revenue/preview")
    @ResponseBody
    PreviewResponse revenuePreview(@RequestParam(required = false) LocalDate startDate,
                                   @RequestParam(required = false) LocalDate endDate) throws IOException, JRException {
        var range = reportData.range(startDate, endDate);
        return createPreview("revenue-report.pdf", jasperReports.revenuePdf(range, reportData.revenue(range)));
    }

    @GetMapping("/monthly-bills/preview")
    @ResponseBody
    PreviewResponse monthlyBillsPreview(@RequestParam(required = false) LocalDate startDate,
                                        @RequestParam(required = false) LocalDate endDate) throws IOException, JRException {
        var range = reportData.range(startDate, endDate);
        return createPreview("monthly-bills-report.pdf", jasperReports.monthlyBillsPdf(range, reportData.monthlyBills(range)));
    }

    @GetMapping("/rooms/preview")
    @ResponseBody
    PreviewResponse roomsPreview() throws IOException, JRException {
        return createPreview("rooms-report.pdf", jasperReports.roomsPdf(reportData.rooms()));
    }

    @GetMapping("/bookings/preview")
    @ResponseBody
    PreviewResponse bookingsPreview(@RequestParam(required = false) LocalDate startDate,
                                    @RequestParam(required = false) LocalDate endDate) throws IOException, JRException {
        var range = reportData.range(startDate, endDate);
        return createPreview("bookings-report.pdf", jasperReports.bookingsPdf(range, reportData.bookings(range)));
    }

    @GetMapping("/deposit-refunds/preview")
    @ResponseBody
    PreviewResponse depositRefundsPreview(@RequestParam(required = false) LocalDate startDate,
                                          @RequestParam(required = false) LocalDate endDate) throws IOException, JRException {
        var range = reportData.range(startDate, endDate);
        return createPreview("deposit-refunds-report.pdf", jasperReports.depositRefundsPdf(range, reportData.depositRefunds(range)));
    }

    @GetMapping("/previews/{previewId}.pdf")
    ResponseEntity<FileSystemResource> previewFile(@org.springframework.web.bind.annotation.PathVariable UUID previewId,
                                                    @RequestParam(defaultValue = "false") boolean download) {
        PreviewFile preview = previewFiles.get(previewId);
        if (preview == null || !Files.isRegularFile(preview.path())) {
            throw new ResponseStatusException(NOT_FOUND);
        }
        return previewResponse(preview, download);
    }

    @GetMapping("/{reportType}/preview.pdf")
    ResponseEntity<FileSystemResource> previewReportPdf(@org.springframework.web.bind.annotation.PathVariable String reportType,
                                                         @RequestParam(required = false) LocalDate startDate,
                                                         @RequestParam(required = false) LocalDate endDate,
                                                         @RequestParam UUID previewId,
                                                         @RequestParam(defaultValue = "false") boolean download) throws IOException, JRException {
        PreviewFile preview = previewFiles.get(previewId);
        if (preview == null || !Files.isRegularFile(preview.path())) {
            preview = createPreviewFile(previewId, reportType, startDate, endDate);
        }
        return previewResponse(preview, download);
    }

    private ResponseEntity<FileSystemResource> previewResponse(PreviewFile preview, boolean download) {
        String disposition = (download ? "attachment" : "inline") + "; filename=\"" + preview.filename() + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(new FileSystemResource(preview.path()));
    }

    private ReportDataService.DateRange addDateRange(Model model, LocalDate startDate, LocalDate endDate, String actionPath) {
        var range = reportData.range(startDate, endDate);
        model.addAttribute("startDate", range.start());
        model.addAttribute("endDate", range.end());
        model.addAttribute("reportAction", actionPath);
        model.addAttribute("reportPdfAction", actionPath + ".pdf");
        model.addAttribute("reportPreviewAction", actionPath + "/preview");
        return range;
    }

    private void addReportFilterModel(Model model, ReportDateSelection selection, String actionPath) {
        model.addAttribute("startDate", selection.range().start());
        model.addAttribute("endDate", selection.range().end());
        model.addAttribute("filterType", selection.filterType());
        model.addAttribute("filterYear", selection.year());
        model.addAttribute("filterMonth", selection.month());
        model.addAttribute("defaultReportYear", LocalDate.now().getYear());
        model.addAttribute("defaultReportMonth", LocalDate.now().getMonthValue());
        model.addAttribute("reportYears", IntStream.rangeClosed(LocalDate.now().getYear() - 5, LocalDate.now().getYear())
                .boxed().sorted(java.util.Comparator.reverseOrder()).toList());
        model.addAttribute("reportMonths", IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new BillingPeriodOption(String.valueOf(month), thaiMonth(month))).toList());
        model.addAttribute("reportAction", actionPath);
        model.addAttribute("reportPdfAction", actionPath + ".pdf");
        model.addAttribute("reportPreviewAction", actionPath + "/preview");
    }

    private ReportDateSelection selectReportDates(String filterType, Integer year, Integer month,
                                                  LocalDate startDate, LocalDate endDate) {
        String selected = switch (filterType == null ? "" : filterType) {
            case "year", "month", "range" -> filterType;
            default -> "all";
        };
        LocalDate today = LocalDate.now();
        if ("year".equals(selected)) {
            int selectedYear = year != null && year >= 2000 && year <= 2100 ? year : today.getYear();
            return new ReportDateSelection(selected, selectedYear, null,
                    new ReportDataService.DateRange(LocalDate.of(selectedYear, 1, 1), LocalDate.of(selectedYear, 12, 31)));
        }
        if ("month".equals(selected)) {
            int selectedYear = year != null && year >= 2000 && year <= 2100 ? year : today.getYear();
            int selectedMonth = month != null && month >= 1 && month <= 12 ? month : today.getMonthValue();
            YearMonth selectedMonthValue = YearMonth.of(selectedYear, selectedMonth);
            return new ReportDateSelection(selected, selectedYear, selectedMonth,
                    new ReportDataService.DateRange(selectedMonthValue.atDay(1), selectedMonthValue.atEndOfMonth()));
        }
        if ("range".equals(selected)) {
            LocalDate selectedStart = startDate != null ? startDate : today.withDayOfMonth(1);
            LocalDate selectedEnd = endDate != null ? endDate : today;
            if (selectedStart.isAfter(selectedEnd)) {
                LocalDate swap = selectedStart;
                selectedStart = selectedEnd;
                selectedEnd = swap;
            }
            return new ReportDateSelection(selected, null, null,
                    new ReportDataService.DateRange(selectedStart, selectedEnd));
        }
        return new ReportDateSelection("all", null, null,
                new ReportDataService.DateRange(LocalDate.of(2000, 1, 1), today));
    }

    private PreviewResponse createPreview(String filename, byte[] pdf) throws IOException {
        cleanupExpiredPreviews();
        UUID id = UUID.randomUUID();
        PreviewFile preview = writePreviewFile(id, filename, pdf);
        String url = "/reports/previews/" + id + ".pdf";
        return new PreviewResponse(url, url + "?download=true");
    }

    private PreviewFile createPreviewFile(UUID id, String reportType, LocalDate startDate, LocalDate endDate) throws IOException, JRException {
        ReportDataService.DateRange range = reportData.range(startDate, endDate);
        return switch (reportType) {
            case "revenue" -> writePreviewFile(id, "revenue-report.pdf", jasperReports.revenuePdf(range, reportData.revenue(range)));
            case "monthly-bills" -> writePreviewFile(id, "monthly-bills-report.pdf", jasperReports.monthlyBillsPdf(range, reportData.monthlyBills(range)));
            case "rooms" -> writePreviewFile(id, "rooms-report.pdf", jasperReports.roomsPdf(reportData.rooms()));
            case "bookings" -> writePreviewFile(id, "bookings-report.pdf", jasperReports.bookingsPdf(range, reportData.bookings(range)));
            case "deposit-refunds" -> writePreviewFile(id, "deposit-refunds-report.pdf", jasperReports.depositRefundsPdf(range, reportData.depositRefunds(range)));
            default -> throw new ResponseStatusException(NOT_FOUND);
        };
    }

    private PreviewFile writePreviewFile(UUID id, String filename, byte[] pdf) throws IOException {
        cleanupExpiredPreviews();
        Path file = Files.createTempFile("hotel-report-", ".pdf");
        Files.write(file, pdf);
        PreviewFile preview = new PreviewFile(file, filename, Instant.now());
        previewFiles.put(id, preview);
        return preview;
    }

    private void cleanupExpiredPreviews() {
        Instant expiry = Instant.now().minus(Duration.ofMinutes(30));
        previewFiles.entrySet().removeIf(entry -> {
            if (entry.getValue().createdAt().isAfter(expiry)) return false;
            try {
                Files.deleteIfExists(entry.getValue().path());
            } catch (IOException ignored) {
                // The operating system will remove remaining temporary files on restart.
            }
            return true;
        });
    }

    private void writePdf(HttpServletResponse response, String filename, byte[] pdf, boolean inline) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", (inline ? "inline" : "attachment") + "; filename=" + filename);
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }

    private YearMonth parseBillingPeriod(String period) {
        if (period == null || period.isBlank() || "all".equalsIgnoreCase(period)) {
            return null;
        }
        try {
            return YearMonth.parse(period);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private MonthlyBillSelection selectMonthlyBills(String filterType, String period, Integer year) {
        String selectedFilter = "period".equals(filterType) || "year".equals(filterType) ? filterType : "all";
        if ("period".equals(selectedFilter)) {
            YearMonth selectedPeriod = parseBillingPeriod(period);
            if (selectedPeriod == null) {
                selectedPeriod = YearMonth.now();
            }
            if (selectedPeriod == null) {
                return new MonthlyBillSelection("all", "", null, reportData.monthlyBills(), reportData.range(null, null), true,
                        "ข้อมูลทั้งหมด ถึงวันที่ " + thaiFullDate(LocalDate.now()));
            }
            return new MonthlyBillSelection(
                    "period",
                    selectedPeriod.toString(),
                    null,
                    reportData.monthlyBills(selectedPeriod.getMonthValue(), selectedPeriod.getYear()),
                    new ReportDataService.DateRange(selectedPeriod.atDay(1), selectedPeriod.atEndOfMonth()),
                    false,
                    "ข้อมูลภายในเดือน " + thaiMonth(selectedPeriod.getMonthValue()) + " " + (selectedPeriod.getYear() + 543)
            );
        }
        if ("year".equals(selectedFilter)) {
            int selectedYear = year != null && year >= 2000 && year <= 2100
                    ? year
                    : LocalDate.now().getYear();
            return new MonthlyBillSelection(
                    "year",
                    "",
                    selectedYear,
                    reportData.monthlyBillsByYear(selectedYear),
                    new ReportDataService.DateRange(LocalDate.of(selectedYear, 1, 1), LocalDate.of(selectedYear, 12, 31)),
                    true,
                    "ข้อมูลภายในปี " + (selectedYear + 543)
            );
        }
        return new MonthlyBillSelection("all", "", null, reportData.monthlyBills(), reportData.range(null, null), true,
                "ข้อมูลทั้งหมด ถึงวันที่ " + thaiFullDate(LocalDate.now()));
    }

    private List<BillingPeriodOption> billingPeriodOptions() {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(YearMonth.now()),
                        monthlyRentBills.findDistinctBillingPeriods().stream()
                                .map(row -> YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue()))
                )
                .distinct()
                .sorted(java.util.Comparator.reverseOrder())
                .map(period -> new BillingPeriodOption(
                        period.toString(),
                        thaiMonth(period.getMonthValue()) + " " + (period.getYear() + 543)
                ))
                .toList();
    }

    private List<BillingYearOption> billingYearOptions() {
        return billingPeriodOptions().stream()
                .map(option -> YearMonth.parse(option.value()).getYear())
                .distinct()
                .map(year -> new BillingYearOption(year, String.valueOf(year + 543)))
                .toList();
    }

    private String thaiMonth(int month) {
        return new String[]{"มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน", "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม"}[month - 1];
    }

    private String reportCondition(ReportDateSelection selection) {
        return switch (selection.filterType()) {
            case "year" -> "ข้อมูลภายในปี " + (selection.year() + 543);
            case "month" -> "ข้อมูลภายในเดือน " + thaiMonth(selection.month()) + " " + (selection.year() + 543);
            case "range" -> "ข้อมูลระหว่างวันที่ " + thaiFullDate(selection.range().start())
                    + " ถึง " + thaiFullDate(selection.range().end());
            default -> "ข้อมูลทั้งหมด ถึงวันที่ " + thaiFullDate(selection.range().end());
        };
    }

    private String thaiFullDate(LocalDate date) {
        return date.getDayOfMonth() + " " + thaiMonth(date.getMonthValue()) + " " + (date.getYear() + 543);
    }

    private record PreviewFile(Path path, String filename, Instant createdAt) {
    }

    private record BillingPeriodOption(String value, String label) {
    }

    private record BillingYearOption(Integer value, String label) {
    }

    private record ReportDateSelection(String filterType, Integer year, Integer month,
                                       ReportDataService.DateRange range) {
    }

    private record MonthlyBillSelection(String filterType,
                                        String period,
                                        Integer year,
                                        ReportDataService.MonthlyBillReport report,
                                        ReportDataService.DateRange range,
                                        boolean showPeriodSummary,
                                        String subtitle) {
    }

    private record PreviewResponse(String url, String downloadUrl) {
    }
}
