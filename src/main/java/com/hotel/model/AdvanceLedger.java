package com.hotel.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_advance_ledger")
public class AdvanceLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_advance_ledger")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_guest", referencedColumnName = "`ID_guest`")
    private Guest guest;

    @ManyToOne
    @JoinColumn(name = "id_room", referencedColumnName = "`ID_room`")
    private Room room;

    @ManyToOne
    @JoinColumn(name = "ID_monthly_rent_bill", referencedColumnName = "ID_monthly_rent_bill")
    private MonthlyRentBill bill;

    @ManyToOne
    @JoinColumn(name = "id_payment", referencedColumnName = "`ID_payment`")
    private Payment payment;

    @Column(name = "id_advance_ledger_type", nullable = false)
    private Long typeId = 1L;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_advance_ledger_type", insertable = false, updatable = false)
    private AdvanceLedgerTypeMaster typeMaster;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "balance_before", nullable = false)
    private BigDecimal balanceBefore = BigDecimal.ZERO;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public MonthlyRentBill getBill() { return bill; }
    public void setBill(MonthlyRentBill bill) { this.bill = bill; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public String getType() { return typeMaster != null ? typeMaster.getCode() : (typeId != null && typeId == 2L ? "APPLY_ADVANCE" : typeId != null && typeId == 3L ? "REFUND_ADVANCE" : typeId != null && typeId == 4L ? "ADJUST_ADVANCE" : "ADD_ADVANCE"); }
    public void setType(String type) { this.typeId = switch (type == null ? "ADD_ADVANCE" : type) { case "APPLY_ADVANCE" -> 2L; case "REFUND_ADVANCE" -> 3L; case "ADJUST_ADVANCE" -> 4L; default -> 1L; }; }
    public Long getTypeId() { return typeId; }
    public void setTypeId(Long id) { this.typeId = id; }
    public AdvanceLedgerTypeMaster getTypeMaster() { return typeMaster; }
    public void setTypeMaster(AdvanceLedgerTypeMaster value) { this.typeMaster = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
