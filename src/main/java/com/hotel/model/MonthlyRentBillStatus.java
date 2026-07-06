package com.hotel.model;

public enum MonthlyRentBillStatus {
    DRAFT(null, "ยังไม่ออกบิล"),
    PENDING(1L, "รอชำระ"),
    PARTIAL_PAID(2L, "ชำระบางส่วน"),
    PAID(3L, "ชำระครบ"),
    CANCELLED(4L, "ยกเลิก"),
    OVERDUE(null, "เกินกำหนด");

    private final Long id;
    private final String label;

    MonthlyRentBillStatus(Long id, String label) {
        this.id = id;
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public static MonthlyRentBillStatus fromId(Long id) {
        if (id == null) {
            return DRAFT;
        }
        for (MonthlyRentBillStatus status : values()) {
            if (id.equals(status.id)) {
                return status;
            }
        }
        return DRAFT;
    }

    public static Long idOf(MonthlyRentBillStatus status) {
        return status == null ? null : status.id;
    }
}
