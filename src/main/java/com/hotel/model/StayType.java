package com.hotel.model;

public enum StayType {
    DAILY("รายวัน"),
    MONTHLY("รายเดือน");

    private final String label;

    StayType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
