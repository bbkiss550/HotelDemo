package com.hotel.model;

import java.util.List;

public final class LookupCodes {
    private LookupCodes() {}
    public static final String DAILY = "DAILY";
    public static final String MONTHLY = "MONTHLY";
    public static final String AVAILABLE = "AVAILABLE";
    public static final String OCCUPIED = "OCCUPIED";
    public static final String DAILY_OCCUPIED = "DAILY_OCCUPIED";
    public static final String MONTHLY_OCCUPIED = "MONTHLY_OCCUPIED";
    public static final String RESERVED = "RESERVED";
    public static final String MAINTENANCE = "MAINTENANCE";
    public static final String PAID = "PAID";
    public static final String UNPAID = "UNPAID";
    public static final String PARTIAL = "PARTIAL";
    public static final String VOID = "VOID";
    public static final String PENDING = "PENDING";
    public static final String CHECKED_IN = "CHECKED_IN";
    public static final String CANCELLED = "CANCELLED";
    public static final String DRAFT = "DRAFT";
    public static final String PARTIAL_PAID = "PARTIAL_PAID";
    public static final String ADD_ADVANCE = "ADD_ADVANCE";
    public static final String APPLY_ADVANCE = "APPLY_ADVANCE";
    public static final String ADJUST_ADVANCE = "ADJUST_ADVANCE";
    public static final String FREE = "FREE";
    public static final String FIXED = "FIXED";
    public static final String PER_HOUR = "PER_HOUR";
    public static final String PERCENT_DAILY_RATE = "PERCENT_DAILY_RATE";
    public static final String ADD_NIGHTS = "ADD_NIGHTS";

    public static List<String> roomStatusCodes() {
        return List.of(AVAILABLE, OCCUPIED, DAILY_OCCUPIED, MONTHLY_OCCUPIED, RESERVED, MAINTENANCE);
    }

    public static List<String> stayTypeCodes() { return List.of(DAILY, MONTHLY); }
    public static List<String> paymentStatusCodes() { return List.of(PAID, UNPAID, PARTIAL, VOID); }
    public static List<String> penaltyChargeTypeCodes() { return List.of(FREE, FIXED, PERCENT_DAILY_RATE, ADD_NIGHTS, PER_HOUR); }
}
