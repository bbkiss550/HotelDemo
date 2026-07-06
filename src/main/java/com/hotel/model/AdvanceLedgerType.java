package com.hotel.model;

public enum AdvanceLedgerType {
    ADD_ADVANCE("เพิ่มเงินล่วงหน้า"),
    APPLY_ADVANCE("นำเงินล่วงหน้าไปหักบิล"),
    REFUND_ADVANCE("คืนเงินล่วงหน้า"),
    ADJUST_ADVANCE("ปรับยอดเงินล่วงหน้า");

    private final String label;

    AdvanceLedgerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
