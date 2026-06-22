package com.hotel.model;

public enum RoomStatus {
    AVAILABLE("ว่าง"),
    OCCUPIED("มีผู้พัก"),
    DAILY_OCCUPIED("พักรายวัน"),
    MONTHLY_OCCUPIED("พักรายเดือน"),
    RESERVED("จองแล้ว"),
    MAINTENANCE("ซ่อมบำรุง");

    private final String label;

    RoomStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
