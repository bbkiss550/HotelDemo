package com.hotel.model;

public enum BookingStatus {
    PENDING("รอยืนยัน"),
    CONFIRMED("ยืนยันแล้ว"),
    CHECKED_IN("เข้าพักแล้ว"),
    CANCELLED("ยกเลิก");

    private final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
