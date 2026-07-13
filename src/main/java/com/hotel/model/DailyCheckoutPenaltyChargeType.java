package com.hotel.model;

public enum DailyCheckoutPenaltyChargeType {
    FREE("ไม่คิดค่าบริการ"),
    FIXED("จำนวนเงินคงที่"),
    PER_HOUR("คิดต่อชั่วโมง"),
    PERCENT_DAILY_RATE("เปอร์เซ็นต์ของค่าห้องรายวัน"),
    ADD_NIGHTS("เพิ่มค่าห้องเป็นจำนวนคืน");

    private final String label;

    DailyCheckoutPenaltyChargeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
