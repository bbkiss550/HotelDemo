package com.hotel.model;

/** Compatibility constants for bill status codes. Persistence uses t_bill_status. */
public final class MonthlyRentBillStatus {
    private MonthlyRentBillStatus() {}
    public static final String DRAFT = "DRAFT";
    public static final String PENDING = "PENDING";
    public static final String PARTIAL_PAID = "PARTIAL_PAID";
    public static final String PAID = "PAID";
    public static final String CANCELLED = "CANCELLED";
    public static final String OVERDUE = "OVERDUE";
    public static Long getId(String code) { return switch (code) { case PARTIAL_PAID -> 2L; case PAID -> 3L; case CANCELLED -> 4L; default -> 1L; }; }
    public static String label(String code) { return code == null ? "" : code; }
}
