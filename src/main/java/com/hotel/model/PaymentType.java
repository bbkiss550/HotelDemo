package com.hotel.model;

public enum PaymentType {
    RENT_PAYMENT("ชำระค่าเช่าตามบิล"),
    ADVANCE_PAYMENT("ชำระค่าเช่าล่วงหน้า"),
    DEPOSIT_PAYMENT("เงินประกัน"),
    OTHER_PAYMENT("รายการอื่น ๆ");

    private final String label;

    PaymentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
