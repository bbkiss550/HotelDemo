package com.hotel.model;

public enum PaymentStatus {
    PAID("ชำระแล้ว"),
    UNPAID("ค้างชำระ"),
    PARTIAL("ชำระบางส่วน"),
    VOID("ยกเลิก");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
